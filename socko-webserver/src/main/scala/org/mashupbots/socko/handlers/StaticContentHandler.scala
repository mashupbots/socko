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
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelFutureProgressListener
import org.jboss.netty.channel.DefaultFileRegion
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.spdy.SpdyHttpHeaders
import org.jboss.netty.handler.ssl.SslHandler
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.infrastructure.IOUtil.using
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.HashUtil
import org.mashupbots.socko.infrastructure.IOUtil
import org.mashupbots.socko.infrastructure.LocalCache
import org.mashupbots.socko.infrastructure.MimeTypes
import org.mashupbots.socko.netty.HttpChunkedFile

import akka.actor.Actor
import akka.event.Logging

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
 *   router = actorSystem.actorOf(Props[StaticContentHandler]
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
class StaticContentHandler() extends Actor {

  private val log = Logging(context.system, this)
  private val rootFilePaths = StaticContentHandlerConfig.rootFilePaths
  private val tempDir = StaticContentHandlerConfig.tempDir
  private val cache = StaticContentHandlerConfig.cache
  private val serverCacheMaxFileSize = StaticContentHandlerConfig.serverCacheMaxFileSize
  private val serverCacheTimeoutSeconds = StaticContentHandlerConfig.serverCacheTimeoutSeconds
  private val browserCacheTimeoutSeconds = StaticContentHandlerConfig.browserCacheTimeoutSeconds

  /**
   * Simple Date Formatter that will format dates like: `Wed, 02 Oct 2002 13:00:00 GMT`
   */
  private val dateFormatter = DateUtil.rfc1123DateFormatter

  /**
   * Only takes [[org.mashupbots.socko.handlers.StaticFileRequest]] or
   * messages.
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
    // Check if it is in the cache
    val cachedContent = cache.get(resourceRequest.cacheKey)
    if (cachedContent.isDefined) {
      cachedContent.get match {
        case res: CachedResource => sendResource(resourceRequest, res)
      }
    } else {
      cacheAndSendResource(resourceRequest)
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
      var content = cacheEntry.content
      var contentEncoding = ""

      // Check if compression is supported.
      // if format not supported or there is an error during compression, download uncompressed content
      val supportedEncoding = event.request.supportedEncoding
      if (supportedEncoding.isDefined) {
        val key = "%s[%s][%s]".format(resourceRequest.cacheKey, supportedEncoding.get, cacheEntry.etag)
        val compressedContents = cache.get(key)
        if (compressedContents.isDefined) {
          content = compressedContents.get.asInstanceOf[Array[Byte]]
          contentEncoding = supportedEncoding.get
        } else {
          IOUtil.using(new ByteArrayOutputStream()) { bytesOut =>
            IOUtil.using(new BufferedInputStream(new ByteArrayInputStream(cacheEntry.content))) { bytesIn =>
              if (compress(bytesIn, bytesOut, supportedEncoding.get)) {
                content = bytesOut.toByteArray
                contentEncoding = supportedEncoding.get
                val cacheTimeout = resourceRequest.serverCacheTimeoutSeconds.getOrElse(this.serverCacheTimeoutSeconds) * 1000L
                cache.set(key, content, cacheTimeout)
              }
            }
          }
        }
      }

      // Prepare response    
      val response = new DefaultHttpResponse(HttpVersion.valueOf(event.request.httpVersion), HttpResponseStatus.OK.toNetty)
      setCommonHeaders(event, response, now, cacheEntry.contentType, content.length, contentEncoding)

      val browserCacheSeconds = resourceRequest.browserCacheTimeoutSeconds.getOrElse(browserCacheTimeoutSeconds)
      now.add(Calendar.SECOND, browserCacheSeconds)
      response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(now.getTime))
      response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + browserCacheSeconds)
      response.setHeader(HttpHeaders.Names.ETAG, cacheEntry.etag)

      response.setContent(ChannelBuffers.wrappedBuffer(content))

      // We have to write our own web log entry since we are not using context.writeResponse
      event.writeWebLog(HttpResponseStatus.OK.code, content.length)

      // Write the response
      val ch = event.channel
      val writeFuture: ChannelFuture = ch.write(response)

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
  }

  /**
   * Caches and then sends a file
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
        cache.set(fileRequest.cacheKey, cachedContent, cacheTimeout)
        sendSmallFile(fileRequest, cachedContent)
      } else {
        // Big file so leave contents on file system
        val cachedContent = CachedBigFile(
          filePath,
          contentType,
          new Date(file.lastModified))
        cache.set(fileRequest.cacheKey, cachedContent, cacheTimeout)
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
      var content = cacheEntry.content
      var contentEncoding = ""

      // Check if compression is supported.
      // if format not supported or there is an error during compression, download uncompressed content
      val supportedEncoding = event.request.supportedEncoding
      if (supportedEncoding.isDefined) {
        val key = "%s[%s][%s]".format(fileRequest.cacheKey, supportedEncoding.get, cacheEntry.etag)
        val compressedContents = cache.get(key)
        if (compressedContents.isDefined) {
          content = compressedContents.get.asInstanceOf[Array[Byte]]
          contentEncoding = supportedEncoding.get
        } else {
          IOUtil.using(new ByteArrayOutputStream()) { bytesOut =>
            IOUtil.using(new BufferedInputStream(new ByteArrayInputStream(cacheEntry.content))) { bytesIn =>
              if (compress(bytesIn, bytesOut, supportedEncoding.get)) {
                content = bytesOut.toByteArray
                contentEncoding = supportedEncoding.get
                val cacheTimeout = fileRequest.serverCacheTimeoutSeconds.getOrElse(this.serverCacheTimeoutSeconds) * 1000L
                cache.set(key, content, cacheTimeout)
              }
            }
          }
        }
      }

      // Prepare response    
      val response = new DefaultHttpResponse(HttpVersion.valueOf(event.request.httpVersion), HttpResponseStatus.OK.toNetty)
      setCommonHeaders(event, response, now, cacheEntry.contentType, content.length, contentEncoding)

      val browserCacheSeconds = fileRequest.browserCacheTimeoutSeconds.getOrElse(browserCacheTimeoutSeconds)
      now.add(Calendar.SECOND, browserCacheSeconds)
      response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(now.getTime))
      response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + browserCacheSeconds)
      response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(cacheEntry.lastModified))
      response.setHeader(HttpHeaders.Names.ETAG, cacheEntry.etag)

      response.setContent(ChannelBuffers.wrappedBuffer(content))

      // We have to write our own web log entry since we are not using context.writeResponse
      event.writeWebLog(HttpResponseStatus.OK.code, content.length)

      // Write response
      val ch = event.channel
      val writeFuture: ChannelFuture = ch.write(response)

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
        case _ => true
      }
    })

    if (isModified) {
      val now = new GregorianCalendar()

      var fileToDownload: File = fileRequest.file
      var contentEncoding = ""

      // Check if compression is supported.
      val supportedEncoding = event.request.supportedEncoding
      if (supportedEncoding.isDefined) {
        // We put the file timestamp in the hash to make sure that if a file changes,
        // the latest copy is downloaded 
        val compressedFileName = HashUtil.md5(fileToDownload.getAbsolutePath + "_" + fileToDownload.lastModified) +
          "." + supportedEncoding.get
        val compressedFile = new File(tempDir, compressedFileName)
        if (compressedFile.exists) {
          fileToDownload = compressedFile
          contentEncoding = supportedEncoding.get
        } else {
          IOUtil.using(new FileOutputStream(compressedFile)) { fileOut =>
            IOUtil.using(new BufferedInputStream(new FileInputStream(fileToDownload))) { fileIn =>
              if (compress(fileIn, fileOut, supportedEncoding.get)) {
                fileToDownload = compressedFile
                contentEncoding = supportedEncoding.get
              }
            }
          }
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
        val ch = event.channel
        val sendHttpChunks = (ch.getPipeline().get(classOf[SslHandler]) != null)
        val fileLength = raf.length()

        // Prepare response    
        val response = new DefaultHttpResponse(HttpVersion.valueOf(event.request.httpVersion), HttpResponseStatus.OK.toNetty)
        setCommonHeaders(event, response, now, cacheEntry.contentType, fileLength, contentEncoding)

        val browserCacheSeconds = fileRequest.browserCacheTimeoutSeconds.getOrElse(browserCacheTimeoutSeconds)
        now.add(Calendar.SECOND, browserCacheSeconds)
        response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(now.getTime))
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + browserCacheSeconds)
        response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(cacheEntry.lastModified))

        if (sendHttpChunks) {
          // See http://stackoverflow.com/questions/9027322/how-to-use-chunkedstream-properly
          response.setChunked(true);
          response.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        }

        // We have to write our own web log entry since we are not using context.writeResponse
        event.writeWebLog(HttpResponseStatus.OK.code, fileLength)

        // Write the initial HTTP response line and headers
        ch.write(response)

        // Write the content
        var writeFuture: ChannelFuture = null

        if (sendHttpChunks) {
          // Cannot use zero-copy with HTTPS.
          writeFuture = ch.write(new HttpChunkedFile(raf, 0, fileLength, 8192))
        } else {
          // No encryption - use zero-copy.
          val region = new DefaultFileRegion(raf.getChannel(), 0, fileLength)
          writeFuture = ch.write(region)
          writeFuture.addListener(new ChannelFutureProgressListener() {
            override def operationComplete(future: ChannelFuture) {
              region.releaseExternalResources()
            }
            override def operationProgressed(
              future: ChannelFuture, amount: Long, current: Long, total: Long) {
              log.debug("{}: {} / {} (+{})%n", Array(cacheEntry.path, current, total, amount))
            }
          })
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
   * @returns `true` if content is compressed, `false` if error during compressing or format not suppored
   */
  private def compress(bytesIn: InputStream, bytesOut: OutputStream, format: String): Boolean = {
    try {
      import IOUtil._
      using(format match {
        case "gzip" => new GZIPOutputStream(bytesOut)
        case "deflate" => new DeflaterOutputStream(bytesOut)
        case _ => throw new UnsupportedOperationException("HTTP compression method '" + format + "' not supported")
      }) { compressedOut =>

        // Pipe input to output in 8K chunks
        val buf = new Array[Byte](8192)
        var len = bytesIn.read(buf)
        while (len > 0) {
          compressedOut.write(buf, 0, len)
          len = bytesIn.read(buf)
        }
      }
      true
    } catch {
      case ex => {
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
    response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(now.getTime))

    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType)

    if (event.request.isKeepAlive) {
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, contentLength)
      response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
    }

    if (contentEncoding != "") {
      response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding)
    }

    var spdyId = event.request.headers.getOrElse(SpdyHttpHeaders.Names.STREAM_ID, "")
    if (spdyId != "") {
      log.debug("spdyId {}", spdyId)
      response.setHeader(SpdyHttpHeaders.Names.STREAM_ID, spdyId)
      response.setHeader(SpdyHttpHeaders.Names.PRIORITY, 0);
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
 * @param serverCacheTimeoutSeconds Number of seconds to cache this specific in the server cache. If `None`, the
 *  default value specified on the constructor is used.
 * @param browserCacheTimeoutSeconds Number of seconds to cache the file in the browser. If `None`, the
 *  default value specified on the constructor is used.
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
 * @param serverCacheTimeoutSeconds Number of seconds to cache this specific in the server cache. If `None`, the
 *  default value specified on the constructor is used.
 * @param browserCacheTimeoutSeconds Number of seconds to cache the file in the browser. If `None`, the
 *  default value specified on the constructor is used.
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
 * You **MUST** set these settings before you start [[org.mashupbots.socko.handlers.StaticContentHandler]] as a router
 */
object StaticContentHandlerConfig {

  /**
   * List of root paths from while files can be served.
   *
   * This is enforced to stop relative path type attacks; e.g. `../etc/passwd`
   */
  var rootFilePaths: Seq[String] = Nil

  /**
   * Temporary directory where compressed files can be stored.
   *
   * Defaults to the `java.io.tmpdir` system property.
   */
  var tempDir: File = new File(System.getProperty("java.io.tmpdir"))

  /**
   * Local in memory cache to store files.
   *
   * Defaults to storing 1000 files.
   */
  var cache = new LocalCache(1000, 16)

  /**
   * Maximum size of files to cache in memory; i.e. contents of these files are read and stored in memory in order
   * to optimize performance.
   *
   * Files larger than this are kept on the file system.
   *
   * Defaults to 100K.
   */
  var serverCacheMaxFileSize: Int = 1024 * 100

  /**
   * Number of seconds before files cached in the server memory are removed.
   *
   * Defaults to 1 hour.
   */
  var serverCacheTimeoutSeconds: Int = 3600

  /**
   * Number of seconds before a browser should check back with the server if a file has been updated.
   *
   * This setting is used to drive the `Expires` and `Cache-Control` HTTP headers.
   *
   * Defaults to 1 hour.
   */
  var browserCacheTimeoutSeconds: Int = 3600
}
  
