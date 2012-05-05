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
package org.mashupbots.socko.context

import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.util.CharsetUtil
import javax.activation.MimetypesFileTypeMap
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayOutputStream

/**
 * Abstract context for reading HTTP requests and writing HTTP responses
 */
abstract class HttpProcessingContext() extends ProcessingContext {

  /**
   * Processing configuration
   */
  def config: HttpProcessingConfig

  /**
   * HTTP End point
   */
  def endPoint: EndPoint

  /**
   * `True` if and only if is connection is to be kept alive and the channel should NOT be closed
   * after a response is returned.
   *
   * This flag is controlled by the existence of the keep alive HTTP header.
   * {{{
   * Connection: keep-alive
   * }}}
   */
  def isKeepAlive: Boolean

  /**
   * Array of accepted encoding for content compression from the HTTP header
   *
   * For example, give then header `Accept-Encoding: gzip, deflate`, then an array containing
   * `gzip` and `defalte` will be returned.
   */
  def acceptedEncodings: Array[String]

  /**
   * Sends a string HTTP response to the client with a status of "200 OK".
   *
   * @param content String to send
   * @param contentType MIME content type to set in the response header. Defaults to "text/plain; charset=UTF-8".
   * @param charset Character set encoding. Defaults to "UTF-8"
   * @param headers Additional headers to add to the HTTP response. Defaults to empty map; i.e. no additional headers.
   * @param allowCompression Flag to indicate if content is to be compressed if the `acceptedEncodings` header is set.
   *   Defaults to `true`.
   */
  def writeResponse(
    content: String,
    contentType: String = "text/plain; charset=UTF-8",
    charset: Charset = CharsetUtil.UTF_8,
    headers: Map[String, String] = Map.empty): Unit = {

    writeResponse(content.getBytes(charset), contentType, headers, true)
  }

  /**
   * Sends a binary HTTP response to the client with a status of "200 OK".
   *
   * @param content String to send
   * @param contentType MIME content type to set in the response header. For example, "image/gif"
   * @param headers Additional headers to add to the HTTP response. Defaults to empty map; i.e. no additional headers.
   */
  def writeResponse(
    content: Array[Byte],
    contentType: String,
    headers: Map[String, String]): Unit = {
    writeResponse(content, contentType, headers, true)
  }
  
  /**
   * Sends a binary HTTP response to the client with a status of "200 OK".
   *
   * @param content String to send
   * @param contentType MIME content type to set in the response header. For example, "image/gif"
   * @param headers Additional headers to add to the HTTP response. Defaults to empty map; i.e. no additional headers.
   * @param allowCompression Flag to indicate if content is to be compressed if the `acceptedEncodings` header is set.
   */
  def writeResponse(
    content: Array[Byte],
    contentType: String,
    headers: Map[String, String],
    allowCompression: Boolean): Unit = {

    // Build the response object.
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)

    // Content
    setContent(response, content)

    // Headers
    setDateHeader(response)
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType)
    if (headers != null && !headers.isEmpty) {
      headers.foreach { kv => response.setHeader(kv._1, kv._2) }
    }
    if (this.isKeepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes())
      // Add keep alive header as per HTTP 1.1 specifications
      setKeepAliveHeader(response)
    }

    // Write the response.
    val future = channel.write(response)

    // Close the non-keep-alive connection after the write operation is done.
    if (!this.isKeepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }

  /**
   * Set the content in the HTTP response
   *
   * @param response HTTP response
   * @param content Binary content to return in the response
   * @param allowCompression Flag to indicate if content is to be compressed if the `acceptedEncodings` header is set.
   *   Defaults to `true`.
   */
  private def setContent(response: HttpResponse, content: Array[Byte], allowCompression: Boolean = true) {
    val compressBytes = new ByteArrayOutputStream
    var compressedOut: DeflaterOutputStream = null
    val compressible = (content != null && content.size > config.minCompressibleContentSizeInBytes &&
      config.minCompressibleContentSizeInBytes >= 0 && allowCompression)

    try {
      if (compressible && acceptedEncodings.contains("gzip")) {
        compressedOut = new GZIPOutputStream(compressBytes)
        compressedOut.write(content, 0, content.length)
        compressedOut.close()
        compressedOut = null
        response.setContent(ChannelBuffers.copiedBuffer(compressBytes.toByteArray))
        response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, "gzip")
      } else if (compressible && acceptedEncodings.contains("deflate")) {
        compressedOut = new DeflaterOutputStream(compressBytes)
        compressedOut.write(content, 0, content.length)
        compressedOut.close()
        compressedOut = null
        response.setContent(ChannelBuffers.copiedBuffer(compressBytes.toByteArray))
        response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, "deflate")
      } else {
        // No compression
        response.setContent(ChannelBuffers.copiedBuffer(content))
      }
    } catch {
      // If error, then just write without compression
      case ex => response.setContent(ChannelBuffers.copiedBuffer(content))
    } finally {
      if (compressedOut != null) {
        compressedOut.close()
      }
    }
  }

  /**
   * Sends a HTTP error response to the client.
   *
   * @param status HTTP error status indicating the nature of the error
   * @param forceCloseChannel Option flag to force closing channel. Default is false and channel will only be
   * 	closed if `keep-alive` header is not set.
   * @param msg Optional error message. If not supplied, status description will be used.
   */
  def writeErrorResponse(status: HttpResponseStatus, forceCloseChannel: Boolean = false, msg: String = null) {
    val closeChannel = (!this.isKeepAlive || forceCloseChannel)
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)

    val msgToWrite = if (msg == null) status.toString() else msg
    response.setContent(ChannelBuffers.copiedBuffer("Error: " + msgToWrite + "\r\n", CharsetUtil.UTF_8))

    setDateHeader(response)
    setContentTypeHeader(response, "text/plain; charset=UTF-8")
    if (!closeChannel) {
      setKeepAliveHeader(response)
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes())
    }

    val future = channel.write(response)
    if (closeChannel) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }

  /**
   * Redirects the browser to the specified URL using the 302 HTTP status code.
   *
   * Request
   * {{{
   * GET /index.html HTTP/1.1
   * Host: www.example.com
   * }}}
   *
   * Response
   * {{{
   * HTTP/1.1 302 Found
   * Location: http://www.newurl.org/
   * }}}
   *
   * @param url URL to which the browser will be redirected
   */
  def redirect(url: String) {
    val closeChannel = (!this.isKeepAlive)
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND)

    setDateHeader(response)
    response.setHeader(HttpHeaders.Names.LOCATION, url)

    if (!closeChannel) {
      setKeepAliveHeader(response)
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes())
    }

    val future = channel.write(response)
    if (closeChannel) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }

  /**
   * Sets the Date header in the HTTP response.
   *
   * For example:
   * {{{
   * Date: Tue, 01 Mar 2011 22:44:26 GMT
   * }}}
   *
   * @param response HTTP response
   */
  def setDateHeader(response: HttpResponse) {
    val dateFormatter = new SimpleDateFormat(HttpProcessingContext.HTTP_DATE_FORMAT, Locale.US)
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HttpProcessingContext.HTTP_DATE_GMT_TIMEZONE))

    val time = new GregorianCalendar()
    response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()))
  }

  /**
   * Sets the Date, Last-Modified, Expires and Cache-Control headers in the HTTP Response
   *
   * For example:
   * {{{
   * Date:          Tue, 01 Mar 2011 22:44:26 GMT
   * Last-Modified: Wed, 30 Jun 2010 21:36:48 GMT
   * Expires:       Tue, 01 Mar 2012 22:44:26 GMT
   * Cache-Control: private, max-age=31536000
   * }}}
   *
   * @param response HTTP response
   * @param lastModified When the file was last modified
   * @param browserCacheSeconds Number of seconds for which the file should be cached by the browser
   */
  def setDateAndCacheHeaders(response: HttpResponse, lastModified: Date, browserCacheSeconds: Int) {
    val dateFormatter = new SimpleDateFormat(HttpProcessingContext.HTTP_DATE_FORMAT, Locale.US)
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HttpProcessingContext.HTTP_DATE_GMT_TIMEZONE))

    // Date header
    val time = new GregorianCalendar()
    response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()))

    // Add cache headers
    time.add(Calendar.SECOND, browserCacheSeconds)
    response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()))
    response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + browserCacheSeconds)
    response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(lastModified))
  }

  /**
   * Sets the content type header for the HTTP Response based on the file name extension
   *
   * For example:
   * {{{
   * Content-Type: image/gif
   * }}}
   *
   * This implementation uses <a href="http://docs.oracle.com/javase/6/docs/api/javax/activation/MimetypesFileTypeMap.html">
   * `MimetypesFileTypeMap`</a> and relies on the presence of the file extening in a `mime.types` file.
   *
   * @param response  HTTP response
   * @param file file to extract content type
   */
  def setContentTypeHeader(response: HttpResponse, file: File) {
    val mimeTypesMap = new MimetypesFileTypeMap()
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, mimeTypesMap.getContentType(file.getCanonicalPath))
  }

  /**
   * Sets the content type header for the HTTP Response
   *
   * For example:
   * {{{
   * Content-Type: image/gif
   * }}}
   *
   * @param response HTTP response
   * @param mimeType MIME type
   */
  def setContentTypeHeader(response: HttpResponse, mimeType: String) {
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, mimeType)
  }

  /**
   * Sets the connection header for the HTTP Response
   *
   * According to [[http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Persistent%20Connections HTTP 1.1]]
   * specifications: ''The Connection header field with a keep-alive keyword must be sent on all requests and
   * responses that wish to continue the persistence''.
   *
   * For example:
   * {{{
   * Connection: keep-alive
   * }}}
   *
   * @param response HTTP response
   */
  def setKeepAliveHeader(response: HttpResponse) {
    if (this.isKeepAlive) {
      response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
    }
  }
}

/**
 * Statics
 */
object HttpProcessingContext {
  val HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
  val HTTP_DATE_GMT_TIMEZONE = "GMT"
}