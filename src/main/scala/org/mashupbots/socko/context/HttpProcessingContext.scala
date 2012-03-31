//
// Copyright 2012 Vibul Imtarnasan and David Bolton.
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

/**
 * Abstract context for reading HTTP requests and writing HTTP responses
 */
abstract class HttpProcessingContext() extends ProcessingContext {

  /**
   * `True` if and only the connection is to be left open after processing this request.
   */
  def isKeepAlive: Boolean

  /**
   * Sends a HTTP response to the client with a status of "200 OK".
   *
   * @param content String to send
   * @param contentType MIME content type to set in the response header. Defaults to "text/plain; charset=UTF-8".
   * @param charset Character set encoding. Defaults to "UTF-8"
   * @param headers Additional headers to add to the HTTP response. Defaults to empty map; i.e. no additional headers.
   */
  def writeResponse(
    content: String,
    contentType: String = "text/plain; charset=UTF-8",
    charset: Charset = CharsetUtil.UTF_8,
    headers: Map[String, String] = Map.empty) = {

    // Build the response object.
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    response.setContent(ChannelBuffers.copiedBuffer(content, charset));
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
    if (!headers.isEmpty) {
      headers.foreach { kv => response.setHeader(kv._1, kv._2) }
    }

    if (this.isKeepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes());
      // Add keep alive header as per HTTP 1.1 specifications
      setKeepAliveHeader(response)
    }

    // Write the response.
    val future = channel.write(response);

    // Close the non-keep-alive connection after the write operation is done.
    if (!this.isKeepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
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

    setDateHeader(response);
    setContentTypeHeader(response, "text/plain; charset=UTF-8")
    if (!closeChannel) {
      setKeepAliveHeader(response)
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes());
    }

    // Close the connection as soon as the error message is sent.
    val future = channel.write(response);
    if (closeChannel) {
      future.addListener(ChannelFutureListener.CLOSE);
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
    time.add(Calendar.SECOND, browserCacheSeconds);
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