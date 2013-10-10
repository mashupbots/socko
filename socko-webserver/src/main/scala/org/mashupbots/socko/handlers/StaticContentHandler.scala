//
// Copyright 2012 Vibul Imtarnasan, David Bolton and Socko contributors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.mashupbots.socko.handlers

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.infrastructure.ConfigUtil
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.HashUtil
import org.mashupbots.socko.infrastructure.IOUtil
import org.mashupbots.socko.infrastructure.IOUtil.using
import org.mashupbots.socko.infrastructure.LocalCache
import org.mashupbots.socko.infrastructure.MimeTypes
import org.mashupbots.socko.netty.HttpChunkedFile

import com.typesafe.config.Config

import akka.actor.Actor
import akka.actor.Extension
import akka.event.Logging
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelProgressiveFuture
import io.netty.channel.ChannelProgressiveFutureListener
import io.netty.channel.DefaultFileRegion
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.spdy.SpdyHttpHeaders
import io.netty.handler.ssl.SslHandler

/**
 * Handles downloading of static files and resources.
 *
 * To trigger a download, send a [[org.mashupbots.socko.handlers.StaticFileRequest]] or
 * [[org.mashupbots.socko.handlers.StaticResourceRequest]] message from your routes.
 *
 * [[org.mashupbots.socko.handlers.StaticContentHandler]] performs HTTP compression and also uses the
 * `If-Modified-Since` header for browser side caching.
 *
 * ==Configuration==
 * [[org.mashupbots.socko.handlers.StaticContentHandler]] uses lots of disk IO (blocking code). Please run it using a
 * router with `PinnedDispatcher` (a thread per actor) or `BalancingDispatcher`.
 *
 * For example:
 * {{{
 *   router = actorSystem.actorOf(Props(new StaticContentHandler(StaticContentHandlerConfig()))
 *            .withRouter(FromConfig()).withDispatcher("myDispatcher"), "myRouter")
 * }}}
 *
 * Configuration in `application.conf`:
 * {{{
 *   myDispatcher {
 *     executor = "thread-pool-executor"
 *     type = PinnedDispatcher
 *   }
 *
 *   akka {
 *     event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
 *     loglevel=DEBUG
 *     actor {
 *       deployment {
 *         /myRouter {
 *           router = round-robin
 *           nr-of-instances = 5
 *         }
 *       }
 *     }
 *   }
 * }}}
 *
 * ==Caching Small Files==
 * HTTP `ETag` header is used.
 *
 * {{{
 * Request #1 Headers
 * ------------------
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ------------------
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * ETag: "686897696a7c876b7e"
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ------------------
 * GET /file1.txt HTTP/1.1
 * If-None-Match: "686897696a7c876b7e"
 *
 * Response #2 Headers
 * ------------------
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 * }}}
 *
 * ==Caching Big Files==
 * HTTP `If-Modified-Since` header is used
 *
 * {{{
 * Request #1 Headers
 * ------------------
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ------------------
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ------------------
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ------------------
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 * }}}
 *
 * ==Compression==
 * HTTP `Accept-Encoding` header is used by the caller to nominate their supported compression
 * algorithm. We return the compression format used in the `Content-Encoding` header.
 *
 * {{{
 * Request
 * -------
 * GET /encrypted-area HTTP/1.1
 * Host: www.example.com
 * Accept-Encoding: gzip, deflate
 *
 * Response
 * --------
 * HTTP/1.1 200 OK
 * Content-Encoding: gzip
 * }}}
 *
 * ==Note==
 * If a response includes both an Expires header and a max-age directive, the max-age directive overrides the Expires
 * header, even if the Expires header is more restrictive
 *
 * See [[http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html HTTP Header Field Definitions]]
 */
class StaticContentHandler(defaultConfig: StaticContentHandlerConfig) extends Actor {

  private val log = Logging(context.system, this)
  private val rootFilePaths = defaultConfig.rootFilePaths
  private val tempDir = defaultConfig.tempDir
  private val cache = defaultConfig.cache
  private val serverCacheMaxFileSize = defaultConfig.serverCacheMaxFileSize
  private val serverCacheTimeoutSeconds = defaultConfig.serverCacheTimeoutSeconds
  private val browserCacheTimeoutSeconds = defaultConfig.browserCacheTimeoutSeconds

  /**
   * Simple Date Formatter that will format dates like: `Wed, 02 Oct 2002 13:00:00 GMT`
   */
  private val dateFormatter = DateUtil.rfc1123DateFormatter

  /**
   * Process the request.
   *
   * Only takes [[org.mashupbots.socko.handlers.StaticFileRequest]] or
   * [[org.mashupbots.socko.handlers.StaticResourceRequest]] messages.
   */
  def receive = {
    case request: StaticFileRequest => {
        processStaticFileRequest(request)
      }
    case request: StaticResourceRequest => {
        processStaticResourceRequest(request)
      }
    case msg => {
        log.info("received unknown message of type: {}", msg.getClass.getName)
      }
  }

  /**
   * Downloads the requested file with content compression and caching
   *
   * @param resourceRequest Resource request request
   */
  private def processStaticResourceRequest(resourceRequest: StaticResourceRequest): Unit = {
    val event = resourceRequest.event
    try {
      // Check if it is in the cache
      val cachedContent = cache.get(resourceRequest.cacheKey)
      if (cachedContent.isDefined) {
        cachedContent.get match {
          case res: CachedResource => sendResource(resourceRequest, res)
        }
      } else {
        cacheAndSendResource(resourceRequest)
      }
    } catch {
      case e: Exception => {
          log.error(e, "Error processing static resource request")
          event.response.write(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }
    }

  }

  /**
   * Caches and then sends a resource. Assume that all resources are small and can be stored in memory
   *
   * @param resourceRequest Resource request
   */
  private def cacheAndSendResource(resourceRequest: StaticResourceRequest): Unit = {
    val event = resourceRequest.event

    log.debug("Getting Resource {}", resourceRequest.classpath)

    val contentType = MimeTypes.get(resourceRequest.classpath)
    val cacheTimeout = resourceRequest.serverCacheTimeoutSeconds.getOrElse(this.serverCacheTimeoutSeconds) * 1000L

    val contents = IOUtil.readResource(resourceRequest.classpath)
    if (contents == null) {
      event.response.write(HttpResponseStatus.NOT_FOUND)
    } else {
      val cachedContent = CachedResource(
        resourceRequest.classpath,
        contentType,
        "\"" + HashUtil.md5(contents) + "\"",
        contents)
      cache.set(resourceRequest.cacheKey, cachedContent, cacheTimeout)
      sendResource(resourceRequest, cachedContent)
    }
  }

  /**
   * Download a small file where its contents is stored in the cache
   *
   * @param resourceRequest File request
   * @param cacheEntry Cached entry associated with the resource to send
   */
  private def sendResource(resourceRequest: StaticResourceRequest, cacheEntry: CachedResource): Unit = {
    val event = resourceRequest.event

    val etag = event.request.headers.getOrElse(HttpHeaders.Names.IF_NONE_MATCH, "")
    val isModified = (etag != cacheEntry.etag)
    if (isModified) {
      val now = new GregorianCalendar()

      // Check if compression is supported.
      // if format not supported or there is an error during compression, download uncompressed content
      val (data: Array[Byte], encoding: String) = {
        val supportedEncoding = event.request.supportedEncoding
        if (supportedEncoding.isDefined) {
          val key = "%s[%s][%s]".format(resourceRequest.cacheKey, supportedEncoding.get, cacheEntry.etag)
          val compressedContents = cache.get(key)
          if (compressedContents.isDefined) {
            (compressedContents.get.asInstanceOf[Array[Byte]], supportedEncoding.get)
          } else {
            IOUtil.using(new ByteArrayOutputStream()) { bytesOut =>
              IOUtil.using(new BufferedInputStream(new ByteArrayInputStream(cacheEntry.content))) { bytesIn =>
                if (compress(bytesIn, bytesOut, supportedEncoding.get)) {
                  val content = bytesOut.toByteArray
                  val cacheTimeout = resourceRequest.serverCacheTimeoutSeconds.getOrElse(this.serverCacheTimeoutSeconds) * 1000L
                  cache.set(key, content, cacheTimeout)
                  (content, supportedEncoding.get)
                } else {
                  (cacheEntry.content, "")
                }
              }
            }
          }
        } else {
          (cacheEntry.content, "")
        }
      }

      // Prepare response    
      val response = new DefaultFullHttpResponse(
        HttpVersion.valueOf(event.request.httpVersion),
        HttpResponseStatus.OK.toNetty,
        event.context.alloc.buffer(data.length).writeBytes(data)
      )
      setCommonHeaders(event, response, now, cacheEntry.contentType, data.length, encoding)

      val browserCacheSeconds = resourceRequest.browserCacheTimeoutSeconds.getOrElse(browserCacheTimeoutSeconds)
      if (browserCacheSeconds > 0) {
        now.add(Calendar.SECOND, browserCacheSeconds)
        response.headers.set(HttpHeaders.Names.EXPIRES, dateFormatter.format(now.getTime))
        response.headers.set(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + browserCacheSeconds)
        response.headers.set(HttpHeaders.Names.ETAG, cacheEntry.etag)
      }

      // We have to write our own web log entry since we are not using context.writeResponse
      event.writeWebLog(HttpResponseStatus.OK.code, data.length)

      // Write the response
      val ctx = event.context
      val writeFuture: ChannelFuture = ctx.writeAndFlush(response)

      // Decide whether to close the connection or not.
      if (!event.request.isKeepAlive) {
        writeFuture.addListener(ChannelFutureListener.CLOSE)
      }
    } else {
      event.response.write(HttpResponseStatus.NOT_MODIFIED)
    }
  }

  /**
   * Downloads the requested file with content compression and caching
   *
   * @param fileRequest File request
   */
  private def processStaticFileRequest(fileRequest: StaticFileRequest): Unit = {
    val file = fileRequest.file
    val filePath = file.getAbsolutePath
    val event = fileRequest.event

    try {
      // Check if it is in the cache
      val cachedContent = cache.get(fileRequest.cacheKey)
      if (cachedContent.isDefined) {
        cachedContent.get match {
          case smallFile: CachedSmallFile => sendSmallFile(fileRequest, smallFile)
          case bigFile: CachedBigFile => sendBigFile(fileRequest, bigFile)
        }
      } else {
        cacheAndSendFile(fileRequest)
      }
    } catch {
      case e: Exception => {
          log.error(e, "Error processing static file request")
          event.response.write(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }
    }
  }

  /**
   * Caches (if specified) and then sends a file
   *
   * @param fileRequest File request
   */
  private def cacheAndSendFile(fileRequest: StaticFileRequest): Unit = {
    val file = fileRequest.file
    val filePath = file.getAbsolutePath
    val event = fileRequest.event

    // Not in the cache so cache and send ...
    if (!rootFilePaths.exists(p => filePath.startsWith(p))) {
      // ".\file.txt" is a path but is not an absolute path nor canonical path.
      // "C:\temp\file.txt" is a path, an absolute path, a canonical path
      // "C:\temp\myapp\bin\..\..\file.txt" is a path, and an absolute path but not a canonical path
      log.debug("File '{}' not under permitted root paths", filePath)
      event.response.write(HttpResponseStatus.NOT_FOUND)
    } else if (!file.exists() || file.isHidden()) {
      log.debug("File '{}' does not exist or is hidden", filePath)
      event.response.write(HttpResponseStatus.NOT_FOUND)
    } else if (file.getName.startsWith(".")) {
      log.debug("File name '{}' starts with .", filePath)
      event.response.write(HttpResponseStatus.NOT_FOUND)
    } else if (!file.isFile()) {
      log.debug("File '{}' is not a file", filePath)
      event.response.write(HttpResponseStatus.NOT_FOUND)
    } else {
      log.debug("Getting File {}", file)

      val contentType = MimeTypes.get(file)
      val cacheTimeout = fileRequest.serverCacheTimeoutSeconds.getOrElse(this.serverCacheTimeoutSeconds) * 1000L
      if (file.length <= serverCacheMaxFileSize) {
        // Small file so cache contents in memory
        val contents = IOUtil.readFile(file)
        val cachedContent = CachedSmallFile(
          filePath,
          contentType,
          "\"" + HashUtil.md5(contents) + "\"",
          new Date(file.lastModified),
          contents)
        if (cacheTimeout > 0) {
          cache.set(fileRequest.cacheKey, cachedContent, cacheTimeout)
        }
        sendSmallFile(fileRequest, cachedContent)
      } else {
        // Big file so leave contents on file system
        val cachedContent = CachedBigFile(
          filePath,
          contentType,
          new Date(file.lastModified))
        if (cacheTimeout > 0) {
          cache.set(fileRequest.cacheKey, cachedContent, cacheTimeout)
        }
        sendBigFile(fileRequest, cachedContent)
      }
    }
  }

  /**
   * Download a small file where its contents is stored in the cache
   *
   * @param fileRequest File request
   * @param cacheEntry Cached entry associated with the file to send
   */
  private def sendSmallFile(fileRequest: StaticFileRequest, cacheEntry: CachedSmallFile): Unit = {
    val event = fileRequest.event

    val etag = event.request.headers.getOrElse(HttpHeaders.Names.IF_NONE_MATCH, "")
    val isModified = (etag != cacheEntry.etag)
    if (isModified) {
      val now = new GregorianCalendar()

      // Check if compression is supported.
      // if format not supported or there is an error during compression, download uncompressed content
      val (data: Array[Byte], encoding: String) = {
        val supportedEncoding = event.request.supportedEncoding
        if (supportedEncoding.isDefined) {
          val key = "%s[%s][%s]".format(fileRequest.cacheKey, supportedEncoding.get, cacheEntry.etag)
          val compressedContents = cache.get(key)
          if (compressedContents.isDefined) {
            (compressedContents.get.asInstanceOf[Array[Byte]], supportedEncoding.get)
          } else {
            IOUtil.using(new ByteArrayOutputStream()) { bytesOut =>
              IOUtil.using(new BufferedInputStream(new ByteArrayInputStream(cacheEntry.content))) { bytesIn =>
                if (compress(bytesIn, bytesOut, supportedEncoding.get)) {
                  val data = bytesOut.toByteArray
                  val cacheTimeout = fileRequest.serverCacheTimeoutSeconds.getOrElse(this.serverCacheTimeoutSeconds) * 1000L
                  cache.set(key, data, cacheTimeout)
                  (data, supportedEncoding.get)
                } else {
                  (cacheEntry.content, "")
                }
              }
            }
          }
        } else {
          (cacheEntry.content, "")
        }
      }

      // Prepare response    
      val response = new DefaultFullHttpResponse(
        HttpVersion.valueOf(event.request.httpVersion),
        HttpResponseStatus.OK.toNetty,
        event.context.alloc.buffer(data.length).writeBytes(data)
      )
      setCommonHeaders(event, response, now, cacheEntry.contentType, data.length, encoding)

      val browserCacheSeconds = fileRequest.browserCacheTimeoutSeconds.getOrElse(browserCacheTimeoutSeconds)
      if (browserCacheSeconds > 0) {
        now.add(Calendar.SECOND, browserCacheSeconds)
        response.headers.set(HttpHeaders.Names.EXPIRES, dateFormatter.format(now.getTime))
        response.headers.set(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + browserCacheSeconds)
        response.headers.set(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(cacheEntry.lastModified))
        response.headers.set(HttpHeaders.Names.ETAG, cacheEntry.etag)
      }

      // We have to write our own web log entry since we are not using context.writeResponse
      event.writeWebLog(HttpResponseStatus.OK.code, data.length)

      // Write response
      val writeFuture: ChannelFuture = event.context.writeAndFlush(response)

      // Decide whether to close the connection or not.
      if (!event.request.isKeepAlive) {
        writeFuture.addListener(ChannelFutureListener.CLOSE)
      }
    } else {
      event.response.write(HttpResponseStatus.NOT_MODIFIED)
    }
  }

  /**
   * Download a big file where its contents is located in the file system
   *
   * @param fileRequest File request
   * @param cacheEntry Cached entry associated with the file to send
   */
  private def sendBigFile(fileRequest: StaticFileRequest, cacheEntry: CachedBigFile): Unit = {
    val event = fileRequest.event

    val ifModifiedSince = event.request.headers.get(HttpHeaders.Names.IF_MODIFIED_SINCE)
    val isModified = (ifModifiedSince.isEmpty || {
        try {
          val ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince.get)
          ifModifiedSinceDate.before(cacheEntry.lastModified)
        } catch {
          case _: Throwable => true
        }
      })

    if (isModified) {
      val now = new GregorianCalendar()

      // Check if compression is supported.
      val (fileToDownload: File, encoding: String) = {
        val supportedEncoding = event.request.supportedEncoding
        if (supportedEncoding.isDefined) {
          // We put the file timestamp in the hash to make sure that if a file changes,
          // the latest copy is downloaded 
          val compressedFileName = HashUtil.md5(fileRequest.file.getAbsolutePath + "_" + fileRequest.file.lastModified) +
          "." + supportedEncoding.get
          val compressedFile = new File(tempDir, compressedFileName)
          if (compressedFile.exists) {
            (compressedFile, supportedEncoding.get)
          } else {
            IOUtil.using(new FileOutputStream(compressedFile)) { fileOut =>
              IOUtil.using(new BufferedInputStream(new FileInputStream(fileRequest.file))) { fileIn =>
                if (compress(fileIn, fileOut, supportedEncoding.get)) {
                  (compressedFile, supportedEncoding.get)
                } else {
                  (fileRequest.file, "")
                }
              }
            }
          }
        } else {
          (fileRequest.file, "")
        }
      }

      // Download file
      val raf: RandomAccessFile = try {
        new RandomAccessFile(fileToDownload, "r")
      } catch {
        case e: FileNotFoundException => {
            event.response.write(HttpResponseStatus.NOT_FOUND)
            null
          }
      }
      if (raf != null) {
        val ctx = event.context
        val sendHttpChunks = (ctx.pipeline.get(classOf[SslHandler]) != null)
        val fileLength = raf.length()

        // Prepare response    
        val response = new DefaultHttpResponse(HttpVersion.valueOf(event.request.httpVersion), HttpResponseStatus.OK.toNetty)
        setCommonHeaders(event, response, now, cacheEntry.contentType, fileLength, encoding)

        val browserCacheSeconds = fileRequest.browserCacheTimeoutSeconds.getOrElse(browserCacheTimeoutSeconds)
        if (browserCacheSeconds > 0) {
          // Don't use ETAG because big files takes a long time to hash. Just use last modified.
          now.add(Calendar.SECOND, browserCacheSeconds)
          response.headers.set(HttpHeaders.Names.EXPIRES, dateFormatter.format(now.getTime))
          response.headers.set(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + browserCacheSeconds)
          response.headers.set(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(cacheEntry.lastModified))
        }

        if (sendHttpChunks) {
          response.headers.set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        }

        // We have to write our own web log entry since we are not using context.writeResponse
        event.writeWebLog(HttpResponseStatus.OK.code, fileLength)

        // Write the initial HTTP response line and headers
        ctx.writeAndFlush(response)

        // Write the content
        val writeFuture: ChannelFuture = {
          if (sendHttpChunks) {
            // Cannot use zero-copy with HTTPS.
            // Cannot not use standard netty ChunkedFile because we have set transfer-encoding to chunked
            // and this means that HTTP chunks (and last chunk) are expected and not chunks of binary.
            ctx.writeAndFlush(new HttpChunkedFile(raf, 0, fileLength, 8192))
          } else {
            // No encryption - use zero-copy.
            val region = new DefaultFileRegion(raf.getChannel(), 0, fileLength)
            ctx.write(region, ctx.newProgressivePromise).addListener(
              new ChannelProgressiveFutureListener {
                override def operationComplete(future: ChannelProgressiveFuture) {
                  log.debug("{}: Transfer complete.", cacheEntry.path)
                }
                override def operationProgressed(
                  future: ChannelProgressiveFuture, progress: Long, total: Long) {
                  log.debug("{}: {} / {} %n", cacheEntry.path, progress, total)
                }
              }
            )
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
          }
        }

        // Decide whether to close the connection or not.
        if (!event.request.isKeepAlive) {
          writeFuture.addListener(ChannelFutureListener.CLOSE)
        }
      }
    } else {
      event.response.write(HttpResponseStatus.NOT_MODIFIED)
    }
  }

  /**
   * Compress the specified content. If we don't support the specified format, `false` is returned.
   *
   * @param bytesIn Stream containing data to compress
   * @param bytesOut Stream to store compressed output
   * @param format Compression formation: "gzip" or "deflate"
   * @return `true` if content is compressed, `false` if error during compressing or format not suppored
   */
  private def compress(bytesIn: InputStream, bytesOut: OutputStream, format: String): Boolean = {
    try {
      using(format match {
          case "gzip" => new GZIPOutputStream(bytesOut)
          case "deflate" => new DeflaterOutputStream(bytesOut)
          case _ => throw new UnsupportedOperationException("HTTP compression method '" + format + "' not supported")
        }) { compressedOut =>
        IOUtil.pipe(bytesIn, compressedOut)
      }
      true
    } catch {
      case ex: Throwable => {
          log.error(ex, "Compression error")
          false
        }
    }
  }

  /**
   * Set common response headers
   *
   * @param event Request event
   * @param response Response to writer header
   * @param now Timestamp
   * @param contentType MIME type of content
   * @param contentLength Length of content
   * @param contentEncoding Encoding code. e.g. `gzip`
   */
  private def setCommonHeaders(event: HttpRequestEvent, response: HttpResponse,
                               now: Calendar, contentType: String, contentLength: Long, contentEncoding: String) {
    response.headers.set(HttpHeaders.Names.DATE, dateFormatter.format(now.getTime))

    response.headers.set(HttpHeaders.Names.CONTENT_TYPE, contentType)

    if (event.request.isKeepAlive) {
      response.headers.set(HttpHeaders.Names.CONTENT_LENGTH, contentLength)
      response.headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
    }

    if (contentEncoding != "") {
      response.headers.set(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding)
    }

    val spdyId = event.request.headers.getOrElse(SpdyHttpHeaders.Names.STREAM_ID, "")
    if (spdyId != "") {
      log.debug("spdyId {}", spdyId)
      response.headers.set(SpdyHttpHeaders.Names.STREAM_ID, spdyId)
      response.headers.set(SpdyHttpHeaders.Names.PRIORITY, 0);
    }
  }

  /**
   * Data structure that we use for caching
   *
   * @param path The path to the file or resource
   * @param contentType MIME content type
   */
  private[StaticContentHandler] trait CachedContent {
    def path: String
    def contentType: String
  }

  /**
   * Data structure that we use for caching resources
   *
   * @param path The path to the file or resource
   * @param contentType MIME content type
   * @param etag Hash id of the file or resource content
   * @param content In memory store of the contents
   */
  private[StaticContentHandler] case class CachedResource(
    path: String,
    contentType: String,
    etag: String,
    content: Array[Byte]) extends CachedContent

  /**
   * Data structure that we use for caching small files which has content loaded into memory
   *
   * @param path The path to the file or resource
   * @param contentType MIME content type
   * @param etag Hash id of the file or resource content
   * @param lastModified Date when this object was last modified
   * @param content In memory store of the contents
   */
  private[StaticContentHandler] case class CachedSmallFile(
    path: String,
    contentType: String,
    etag: String,
    lastModified: Date,
    content: Array[Byte]) extends CachedContent

  /**
   * Data structure that we use for caching information about big file that are stored in the file system
   *
   * @param path The path to the file or resource
   * @param contentType MIME content type
   * @param lastModified Date when this object was last modified
   */
  private[StaticContentHandler] case class CachedBigFile(
    path: String,
    contentType: String,
    lastModified: Date) extends CachedContent

}

/**
 * Message to be sent to [[org.mashupbots.socko.handlers.StaticContentHandler]] for it to download the specified file
 *
 * @param event HTTP Request event
 * @param file File to download to the client
 * @param serverCacheTimeoutSeconds Number of seconds to cache file meta data and contents. If `None`, the
 *  default value specified in [[org.mashupbots.socko.handlers.StaticContentHandlerConfig]] will be used.
 * @param browserCacheTimeoutSeconds Number of seconds the file is to be cached by the browser. If `None`, the
 *  default value specified in [[org.mashupbots.socko.handlers.StaticContentHandlerConfig]] will be used.
 */
case class StaticFileRequest(
  event: HttpRequestEvent,
  file: File,
  serverCacheTimeoutSeconds: Option[Int] = None,
  browserCacheTimeoutSeconds: Option[Int] = None) {

  val cacheKey = "FILE::" + file.getAbsolutePath
}

/**
 * Message to be sent to [[org.mashupbots.socko.handlers.StaticContentHandler]] for it to download the specified file
 *
 * @param event HTTP Request event
 * @param classpath Classpath of resource to load (without a leading "/"). For example: `META-INF/mime.types`.
 * @param serverCacheTimeoutSeconds Number of seconds to cache file meta data and contents. If `None`, the
 *  default value specified in [[org.mashupbots.socko.handlers.StaticContentHandlerConfig]] will be used.
 * @param browserCacheTimeoutSeconds Number of seconds the file is to be cached by the browser. If `None`, the
 *  default value specified in [[org.mashupbots.socko.handlers.StaticContentHandlerConfig]] will be used.
 */
case class StaticResourceRequest(
  event: HttpRequestEvent,
  classpath: String,
  serverCacheTimeoutSeconds: Option[Int] = None,
  browserCacheTimeoutSeconds: Option[Int] = None) {

  val cacheKey = "RES::" + classpath
}

/**
 * Configuration for [[org.mashupbots.socko.handlers.StaticContentHandler]].
 *
 * This can also be loaded from an externalized AKKA configuration file. For example:
 * 
 * {{{
 *   static-content-config {
 *     # CSV list of root paths from while files can be served.
 *     root-file-paths=/home/me/path1,/home/me/path2
 *     
 *     # Temporary directory where compressed files can be stored.
 *     # Defaults to the `java.io.tmpdir` system property if setting is omitted
 *     temp-dir=localhost
 *     
 *     # Max number of files and resources to store in the local in memory cache
 *     # Defaults to storing 1000 items if setting is omitted
 *     server-cache-max-size=1000
 *     
 *     # Maximum size of files (in bytes) to be cached. Files larger than this will
 *     # not be cached and will be served from the file system or resorce.
 *     # Defaults to 100K if setting is omitted
 *     server-cache-max-file-size=102400
 *     
 *     # Number of seconds files will be cached in local memory
 *     # Defaults to 3600 if setting is omitted 
 *     server-cache-timeout=3600
 *     
 *     # Number of seconds before a browser should check back with the server if a file has been updated.
 *     # This setting is used to drive the `Expires` and `Cache-Control` HTTP headers.
 *     # Defaults to 3600 if setting is omitted 
 *     browser-cache-timeout=3600
 *   }
 * }}}
 *
 * can be loaded as follows:
 * {{{
 *   object MyStaticContentHandlerConfig extends ExtensionId[StaticContentHandlerConfig] with ExtensionIdProvider {
 *     override def lookup = MyStaticContentHandlerConfig
 *     override def createExtension(system: ExtendedActorSystem) =
 *       new StaticContentHandlerConfig(system.settings.config, "static-content-config")
 *   }
 *
 *   val myStaticContentHandlerConfig = MyStaticContentHandlerConfig(actorSystem)
 * }}} 
 * 
 * 
 * @param rootFilePaths List of root paths from while files can be served.
 *   This is enforced to stop relative path type attacks; e.g. `../etc/passwd`
 *
 * @param tempDir Temporary directory where compressed files can be stored.
 *   Defaults to the `java.io.tmpdir` system property.
 *
 * @param cache Local in memory cache to store file meta and content.
 *   Defaults to storing 1000 items.
 *
 * @param serverCacheMaxFileSize Maximum size of file contents to cache in memory
 *   The contents of files under this limit is cached in memory for `serverCacheTimeoutSeconds`. Requests for this file
 *   will be served by the contents stored in memory rather than the file system.
 *
 *   Files larger than this limit are served by reading from the file system for every request.
 *
 *   Defaults to 100K. `0` indicates that files will not be cached in memory.
 *
 * @param serverCacheTimeoutSeconds Number of seconds file meta data (such as file name, size, timestamp and hash)
 *   and file contents will be cached in memory.  After this time, cache data will be removed and file meta data will
 *   and contents will have to be read from the file system.
 *
 *   Note that if `serverCacheMaxFileSize` is 0 and `serverCacheTimeoutSeconds` is > 0, then only file meta data
 *   will be cached in memory.
 *
 *   Defaults to 1 hour. `0` means files will NOT be cached.
 *
 * @param browserCacheTimeoutSeconds Number of seconds before a browser should check back with the server if a file has
 *   been updated.
 *
 *   This setting is used to drive the `Expires` and `Cache-Control` HTTP headers.
 *
 *   Defaults to 1 hour. `0` means cache headers will not be sent to the browser.
 */
case class StaticContentHandlerConfig(
  rootFilePaths: Seq[String] = Nil,
  tempDir: File = new File(System.getProperty("java.io.tmpdir")),
  cache: LocalCache = new LocalCache(1000, 16),
  serverCacheMaxFileSize: Int = 1024 * 100,
  serverCacheTimeoutSeconds: Int = 3600,
  browserCacheTimeoutSeconds: Int = 3600) extends Extension {
  
  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    ConfigUtil.getCSV(config, prefix + ".root-file-paths", Nil),
    ConfigUtil.getFile(config, prefix + ".temp-dir", new File(System.getProperty("java.io.tmpdir"))),
    new LocalCache(ConfigUtil.getInt(config, prefix + ".server-cache-max-size", 1000), 16),
    ConfigUtil.getInt(config, prefix + ".server-cache-max-file-size", 1024 * 100),
    ConfigUtil.getInt(config, prefix + ".server-cache-timeout", 3600),
    ConfigUtil.getInt(config, prefix + ".browser-cache-timeout", 3600))
}
