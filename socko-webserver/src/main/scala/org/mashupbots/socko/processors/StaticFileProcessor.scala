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

package org.mashupbots.socko.processors

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import java.util.Date

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaConcurrentMap
import scala.collection.mutable.ConcurrentMap

import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelFutureProgressListener
import org.jboss.netty.channel.DefaultFileRegion
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.utils.Logger

import akka.actor.Actor
import akka.event.Logging

/**
 * A processor that handles downloading of static files.
 *
 * It receives [[org.mashupbots.socko.processors.StaticFileRequest]] messages. 
 *
 * It performs HTTP compression and also uses If-Modified-Since header for caching.
 *
 * This processor contains lots of disk IO (blocking code). Please run it with a router and
 * in its own Akka `PinnedDispatcher` (a thread per actor) or `BalancingDispatcher`.
 *
 * For example:
 * {{{
 *   router = actorSystem.actorOf(Props[StaticFileProcessor]
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
 */
class StaticFileProcessor extends Actor {
  private val log = Logging(context.system, this)

  /**
   * Only takes [[org.mashupbots.socko.processors.StaticFileRequest]] messages. 
   */
  def receive = {
    case request: StaticFileRequest => {
      sendFile(request)
    }
    case _ => {
      log.info("received unknown message of type: ")
    }
  }

  /**
   * Downloads the requested file with content compression and caching
   *
   * ==Caching==
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
   */
  private def sendFile(request: StaticFileRequest): Unit = {
    val file = request.file

    // Checks
    if (!file.getCanonicalPath.startsWith(request.rootFileDir.getCanonicalPath)) {
      // ".\file.txt" is a path but is not an absolute path nor canonical path.
      // "C:\temp\file.txt" is a path, an absolute path, a canonical path
      // "C:\temp\myapp\bin\..\..\file.txt" is a path, and an absolute path but not a canonical path
      log.debug("File '{}' not under root directory '{}'", file.getCanonicalPath, request.rootFileDir.getCanonicalPath)
      request.context.writeErrorResponse(HttpResponseStatus.NOT_FOUND)
      return
    }
    if (!file.exists() || file.isHidden()) {
      log.debug("File '{}' does not exist or is hidden", file.getCanonicalPath)
      request.context.writeErrorResponse(HttpResponseStatus.NOT_FOUND)
      return
    }
    if (file.getName.startsWith(".")) {
      log.debug("File name '{}' starts with .", file.getCanonicalPath)
      request.context.writeErrorResponse(HttpResponseStatus.NOT_FOUND)
      return
    }
    if (!file.isFile()) {
      log.debug("File '{}' is not a file", file.getCanonicalPath)
      request.context.writeErrorResponse(HttpResponseStatus.NOT_FOUND)
      return
    }
    log.debug("Getting {}", file)

    // Download file if it has not been modified
    val lastModified = StaticFileLastModifiedCache.get(file, request.fileLastModifiedCacheTimeoutSeconds)
    if (hasFileBeenModified(request, lastModified)) {
      downloadFile(request, file, lastModified, request.browserCacheTimeoutSeconds)
    } else {
      request.context.writeErrorResponse(HttpResponseStatus.NOT_MODIFIED)
    }
  }

  /**
   * Check if a file has been modified when compared against the timestamp sent by caller
   *
   * @param request Static file request
   * @param lastModified Last modified timestamp
   * @returns `true` if and only if the file at the browser has been modified
   */
  private def hasFileBeenModified(request: StaticFileRequest, lastModified: Long): Boolean = {
    val ifModifiedSinceDate = request.context.getIfModifiedSinceHeader()
    if (ifModifiedSinceDate.isDefined) {
      // When file timestamp is the same as what the browser is sending up
      // Only compare up to the second because the datetime format we send to the client does not have milliseconds 
      val ifModifiedSinceDateSeconds = ifModifiedSinceDate.get.getTime() / 1000
      val fileLastModifiedSeconds = lastModified / 1000
      (ifModifiedSinceDateSeconds != fileLastModifiedSeconds)
    } else {
      // No if-modified-since header set, so we can only assume file has not been downloaded
      true
    }
  }

  /**
   * Download the specified file
   *
   * @param request Static file request
   * @param file File to download
   * @param lastModified Last modified timestamp
   * @param cacheSeconds Number of seconds to set in the cache header
   */
  private def downloadFile(request: StaticFileRequest, file: File, lastModified: Long, cacheSeconds: Int): Unit = {
    var raf: RandomAccessFile = null
    var fileToDownload = file
    var contentEncoding = ""

    // Check if compression is supported.
    // if format not supported or there is an error during compression, download uncompressed file
    if (request.context.acceptedEncodings.length > 0) {
      if (request.context.acceptedEncodings.contains("gzip")) {
        contentEncoding = "gzip"
      } else if (request.context.acceptedEncodings.contains("deflate")) {
        contentEncoding = "deflate"
      }

      if (contentEncoding.length > 0) {
        val compressedFile = compressFile(request, contentEncoding, file)
        if (compressedFile.isDefined) {
          fileToDownload = compressedFile.get
        } else {
          contentEncoding = "" // No compression due to error
        }
      }
    }

    // Get file
    try {
      raf = new RandomAccessFile(fileToDownload, "r")
    } catch {
      case e: FileNotFoundException => {
        request.context.writeErrorResponse(HttpResponseStatus.NOT_FOUND)
      }
    }
    if (raf == null) {
      return
    }
    val fileLength = raf.length()

    // Prepare response
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    HttpHeaders.setContentLength(response, fileLength)
    request.context.setContentTypeHeader(response, file)
    request.context.setKeepAliveHeader(response)
    request.context.setDateAndCacheHeaders(response, new Date(lastModified), cacheSeconds)
    if (contentEncoding != "") {
      response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding)
    }
    
    // We have to write our own web log entry since we are not using request.context.writeResponse
    request.context.writeWebLog(HttpResponseStatus.OK.getCode, fileLength)

    // Write the initial HTTP response line and headers
    val ch = request.context.channel
    ch.write(response)

    // Write the content
    var writeFuture: ChannelFuture = null

    if (ch.getPipeline().get(classOf[SslHandler]) != null) {
      // Cannot use zero-copy with HTTPS.
      writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192))
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
          log.debug("{}: {} / {} (+{})%n", Array(file.getName, current, total, amount))
        }
      })
    }

    // Decide whether to close the connection or not.
    if (!request.context.isKeepAlive) {
      writeFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }

  /**
   * Compress the specified file. If we don't support the specified format, just return the uncompressed file
   *
   * @param request Static file request
   * @param format compression formation: "gzip" or "deflate"
   * @param file file to compress
   * @returns Compressed file or None if compression failed
   */
  private def compressFile(request: StaticFileRequest, format: String, file: File): Option[File] = {
    // We put the file timestamp in the hash to make sure that if a file changes,
    // the latest copy is downloaded 
    val compressedFileName = md5(file.getCanonicalPath + "_" + file.lastModified) + "." + format
    val compressedFile = new File(request.tempDir, compressedFileName)
    var isError = false

    log.debug("Compressed file name: {}", compressedFile.getCanonicalPath)

    if (!compressedFile.exists) {
      var fileIn: BufferedInputStream = null
      var compressedOut: DeflaterOutputStream = null

      try {
        // Create output file
        val fileOut = new FileOutputStream(compressedFile)
        format match {
          case "gzip" => {
            compressedOut = new GZIPOutputStream(fileOut)
          }
          case "deflate" => {
            compressedOut = new DeflaterOutputStream(fileOut)
          }
          case _ => {
            throw new UnsupportedOperationException("HTTP compression method '" + format + "' not supported")
          }
        }

        // Create input file
        fileIn = new BufferedInputStream(new FileInputStream(file))

        // Pipe input to output in 4K chunks
        val buf = new Array[Byte](4096)
        var len = fileIn.read(buf)
        while (len > 0) {
          compressedOut.write(buf, 0, len)
          len = fileIn.read(buf)
        }
      } catch {
        case ex => {
          log.error(ex, "Compression error")
          isError = true
        }
      } finally {
        if (compressedOut != null) {
          compressedOut.close()
        }
        if (fileIn != null) {
          fileIn.close()
        }
      }
    }

    if (isError) None else Some(compressedFile)
  }

  /**
   * Calculate an MD5 has of a string. Used to hashing a file name
   *
   * @param s String to MD5 hash
   * @returns MD5 hash of specified string
   */
  private def md5(s: String): String = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(s.getBytes)
    md5.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }
}

/**
 * Message to be sent to [[org.mashupbots.socko.processors.StaticFileProcessor]] for it to download the specified file
 *
 * @param context HTTP Request context
 * @param rootFileDir Root directory from which files will be served. `file` must be present under this directory; if
 *  not a 404 Not Found is returned. This is used to enforce security to make sure that only intended files can be 
 *  downloaded and prevent paths like `http://foo/../../etc/passwd`.
 * @param file File to download
 * @param tempDir Temporary directory where compressed version of files can be stored
 * @param browserCacheTimeoutSeconds Number of seconds to cache the file in the browser. Defaults to 1 hour.
 * @param fileLastModifiedCacheTimeoutSeconds Number of seconds to cache a file's last modified timestamp.
 *  Defaults to 5 minutes.
 */
case class StaticFileRequest(
  context: HttpRequestProcessingContext,
  rootFileDir: File,
  file: File,
  tempDir: File,
  browserCacheTimeoutSeconds: Int = 3600,
  fileLastModifiedCacheTimeoutSeconds: Int = 300)

/**
 * Cache for the last modified date of files. Caching this value means that we wont have to keep reading the file and
 * hence we reduce blocking IO.
 */
object StaticFileLastModifiedCache extends Logger {
  private val cache: ConcurrentMap[String, FileLastModified] = new ConcurrentHashMap[String, FileLastModified]

  /**
   * Gets a file's last modified date from cache. If not in the cache, the file will be read and its last modified
   * date will be cached
   *
   * @param file File to read last modified date
   * @param timeoutSeconds Seconds before this cache entry expires
   * @return the file's last modified time. `0` is returned if file is not found.
   */
  def get(file: File, timeoutSeconds: Int): Long = {
    require(file != null, "file cannot be null")

    val r = cache.get(file.getCanonicalPath)
    if (r.isDefined && new Date().getTime < r.get.timeout) {
      //log.debug("Getting from cache")
      r.get.lastModified
    } else {
      //log.debug("Read new value")
      cache.put(file.getCanonicalPath,
        FileLastModified(file.getCanonicalPath, file.lastModified, new Date().getTime + (timeoutSeconds * 1000)))
      file.lastModified
    }
  }

  case class FileLastModified(
    filePath: String,
    lastModified: Long,
    timeout: Long)
}

