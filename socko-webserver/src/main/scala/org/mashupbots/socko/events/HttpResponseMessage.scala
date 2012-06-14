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
package org.mashupbots.socko.events

import org.jboss.netty.handler.codec.http.HttpHeaders
import java.nio.charset.Charset
import org.mashupbots.socko.infrastructure.CharsetUtil
import javax.activation.MimetypesFileTypeMap
import java.io.File
import org.jboss.netty.handler.codec.http.HttpResponse
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import java.util.Calendar
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.channel.ChannelFutureListener
import java.util.zip.DeflaterOutputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.DefaultHttpChunk
import org.jboss.netty.handler.codec.http.DefaultHttpChunkTrailer
import org.mashupbots.socko.infrastructure.MimeTypes
import org.mashupbots.socko.infrastructure.DateUtil
import org.jboss.netty.handler.codec.spdy.SpdyHttpHeaders

/**
 * Encapsulates the all the data sent to be sent to the client in an HTTP response; i.e. headers and content.
 *
 * @param event Event associated with this response
 */
case class HttpResponseMessage(event: HttpEvent) {

  /**
   * Request associated with this response
   */
  private val request: HttpRequestMessage = event match {
    case ctx: HttpRequestEvent => ctx.request
    case ctx: HttpChunkEvent => ctx.initialHttpRequest
  }

  /**
   * Flag to indicate if the response has been written to the client
   */
  private var hasBeenWritten = false

  /**
   * Flag to indicate that we are currently writing chunks
   */
  private var writingChunks = false

  /**
   * Counter for size of content sent in chunks
   */
  private var totalChunkContentLength = 0

  /**
   * Aggregate number of bytes sent the client as chunks
   */
  def totalChunkSize: Int = totalChunkContentLength

  /**
   * HTTP Response Status
   */
  var status: HttpResponseStatus = HttpResponseStatus.OK

  /**
   * Headers
   */
  val headers = new scala.collection.mutable.HashMap[String, String]

  /**
   * Content type as MIME code. e.g. `image/gif`. `None` if not set
   */
  def contentType = {
    headers.get(HttpHeaders.Names.CONTENT_TYPE)
  }

  /**
   * Sets the content type header for this HTTP response
   *
   * @param value MIME type. e.g. `image/gif`.
   */
  def contentType_=(value: String) {
    headers.put(HttpHeaders.Names.CONTENT_TYPE, value)
  }

  /**
   * Sends a 100 continue to the client
   */
  def write100Continue() {
    assert(!writingChunks, "Cannot write after writing chunks")
    assert(!hasBeenWritten, "Response has ended")

    val response = new DefaultHttpResponse(HttpVersion.valueOf(request.httpVersion), HttpResponseStatus.CONTINUE.toNetty)
    event.channel.write(response)
  }

  /**
   * Sends a binary HTTP response to the client.
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param content Bytes to send
   */
  def write(content: Array[Byte]): Unit = {
    assert(!writingChunks, "Cannot write after writing chunks")
    assert(!hasBeenWritten, "Response has ended")

    // Build the response object.
    val response = new DefaultHttpResponse(HttpVersion.valueOf(request.httpVersion), status.toNetty)

    // Content
    setContent(response, content, contentType.getOrElse(""))

    // Headers
    HttpResponseMessage.setDateHeader(response)
    HttpResponseMessage.setSpdyHeaders(request, response)
    headers.foreach { kv => response.setHeader(kv._1, kv._2) }

    if (request.isKeepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent.readableBytes)
      // Add keep alive header as per HTTP 1.1 specifications
      HttpResponseMessage.setKeepAliveHeader(response, request.isKeepAlive)
    }

    // Write web log
    event.writeWebLog(response.getStatus.getCode, response.getContent.readableBytes)

    // Write the response.
    val future = event.channel.write(response)

    // Close the non-keep-alive connection after the write operation is done.
    if (!request.isKeepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }

    hasBeenWritten = true;
  }

  /**
   * Sends a string HTTP response to the client
   *
   * The content type will default to `text/plain; charset=UTF-8` if not already set in the headers.
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param content String to send in the response body
   */
  def write(content: String): Unit = {
    if (contentType.isEmpty) {
      contentType = "text/plain; charset=UTF-8"
    }

    val charset = HttpResponseMessage.extractMimeTypeCharset(contentType.get).getOrElse(CharsetUtil.UTF_8)
    write(content.getBytes(charset))
  }

  /**
   * Sends a string HTTP response to the client
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param content String to send in the response body
   * @param contentType MIME content type. If the `charset` is not set, `UTF-8` is assumed
   */
  def write(content: String, contentType: String): Unit = {
    this.contentType = contentType
    write(content)
  }

  /**
   * Sends a string HTTP response to the client
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param content String to send in the response body
   * @param contentType MIME content type. If the `charset` is not set, `UTF-8` is assumed
   * @param headers Headers to add to the HTTP response.
   */
  def write(content: String, contentType: String, headers: Map[String, String]): Unit = {
    this.contentType = contentType
    headers.foreach { kv => this.headers.put(kv._1, kv._2) }
    write(content)
  }

  /**
   * Sends a binary HTTP response to the client
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param content Binary data to send in the response body.
   * @param contentType MIME content type
   */
  def write(content: Array[Byte], contentType: String): Unit = {
    this.contentType = contentType
    write(content)
  }

  /**
   * Sends a binary HTTP response to the client
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param content Binary data to send in the response body.
   * @param contentType MIME content type
   * @param headers Headers to add to the HTTP response. It will be added to the `headers` map.
   */
  def write(content: Array[Byte], contentType: String, headers: Map[String, String]): Unit = {
    this.contentType = contentType
    headers.foreach { kv => this.headers.put(kv._1, kv._2) }
    write(content)
  }

  /**
   * Sends a HTTP response to the client with just the status and not content. This is typically used for an error.
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param status HTTP Status
   */
  def write(status: HttpResponseStatus): Unit = {
    this.status = status
    write(Array.empty[Byte])
  }

  /**
   * Sends a HTTP response to the client with the status as well as a text message. This is typically used for in
   * the event of an error.
   *
   * @param status HTTP Status
   * @param content String to send in the response body. The MIME type will be set to `text/plain; charset=UTF-8`.
   */
  def write(status: HttpResponseStatus, content: String): Unit = {
    this.status = status
    write(content)
  }

  /**
   * Sends a binary HTTP response to the client
   *
   * This write is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `write()` more than once results in an exception being thrown because only 1 response is permitted per
   * request.
   *
   * @param status HTTP Status
   * @param content Binary data to send in the response body
   * @param contentType MIME content type to set in the response header. For example, "image/gif"
   * @param headers Headers to add to the HTTP response. It will be added to the `headers` map.
   */
  def write(
    status: HttpResponseStatus,
    content: Array[Byte],
    contentType: String,
    headers: Map[String, String]): Unit = {

    this.status = status
    this.contentType = contentType
    headers.foreach { kv => this.headers.put(kv._1, kv._2) }

    write(content)
  }

  /**
   * Set the content in the HTTP response
   *
   * @param response HTTP response
   * @param content Binary content to return in the response
   * @param contentType MIME content type to set in the response header. For example, "image/gif"
   */
  private def setContent(response: HttpResponse, content: Array[Byte], contentType: String) {
    // Check to see if we should compress the content
    val compressible = (content != null &&
      content.size > 0 &&
      content.size >= event.config.minCompressibleContentSizeInBytes &&
      content.size <= event.config.maxCompressibleContentSizeInBytes &&
      event.config.compressibleContentTypes.exists(s => contentType.startsWith(s)))

    var compressedOut: DeflaterOutputStream = null
    try {
      if (compressible && request.acceptedEncodings.contains("gzip")) {
        val compressBytes = new ByteArrayOutputStream
        compressedOut = new GZIPOutputStream(compressBytes)
        compressedOut.write(content, 0, content.length)
        compressedOut.close()
        compressedOut = null
        response.setContent(ChannelBuffers.copiedBuffer(compressBytes.toByteArray))
        response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, "gzip")

      } else if (compressible && request.acceptedEncodings.contains("deflate")) {
        val compressBytes = new ByteArrayOutputStream
        compressedOut = new DeflaterOutputStream(compressBytes)
        compressedOut.write(content, 0, content.length)
        compressedOut.close()
        compressedOut = null
        response.setContent(ChannelBuffers.copiedBuffer(compressBytes.toByteArray))
        response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, "deflate")

      } else if (content.size > 0) {
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
   * Initiates a HTTP chunk response to the client
   *
   * Use this method to initiate the response. Call `writeChunk()` to send the individual chunks and finish by
   * calling `writeLastChunk()`.
   *
   * Writing the first chunk is NOT buffered. The chunk is immediately sent to the client.
   *
   * Calling `writeFirstChunk()` more than once results in an exception being thrown because only 1 response is
   * permitted per request.
   *
   * @param contentType MIME content type to set in the response header. For example, "image/gif". If omitted, the
   *  content type set in `headers` will be used.
   * @param headers Headers to add to the HTTP response.
   */
  def writeFirstChunk(
    contentType: String = "",
    headers: Map[String, String] = Map.empty[String, String]): Unit = {

    assert(!writingChunks, "Cannot write after writing chunks")
    assert(!hasBeenWritten, "Response has ended")
    assert(request.httpVersion == HttpVersion.HTTP_1_1.toString, "Chunked enconding only available for HTTP/1.1 requests")

    if (contentType != "") {
      this.contentType = contentType
    }
    headers.foreach { kv => this.headers.put(kv._1, kv._2) }

    // Start totaling chunk length
    totalChunkContentLength = 0

    // Build the response object.
    val response = new DefaultHttpResponse(HttpVersion.valueOf(request.httpVersion), status.toNetty)

    // Headers
    HttpResponseMessage.setDateHeader(response)
    HttpResponseMessage.setSpdyHeaders(request, response)
    this.headers.foreach { kv => response.setHeader(kv._1, kv._2) }
    if (request.isKeepAlive) {
      // Add keep alive header as per HTTP 1.1 specifications
      HttpResponseMessage.setKeepAliveHeader(response, request.isKeepAlive)
    }

    // See http://stackoverflow.com/questions/9027322/how-to-use-chunkedstream-properly
    response.setChunked(true);
    response.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);

    // Write the response
    val future = event.channel.write(response)

    writingChunks = true
  }

  /**
   * Sends a chunk of data to the client.
   *
   * This method must be called AFTER `writeFirstChunk()`.
   *
   * Writing the a chunk is NOT buffered. The chunk is immediately sent to the client.
   *
   * @param chunkConent Binary content to send to the client.
   */
  def writeChunk(chunkContent: Array[Byte]): Unit = {
    assert(writingChunks, "Must call writeFirstChunk() first")
    assert(!hasBeenWritten, "Response has ended")

    totalChunkContentLength += chunkContent.length

    val chunk = new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(chunkContent))
    event.channel.write(chunk)
  }

  /**
   * Sends a chunk to the client.
   *
   * This method must be called AFTER the last `writeChunk()`.
   *
   * Writing the last chunk is NOT buffered. The last chunk is immediately sent to the client.
   *
   * Calling `writeLastChunk()` more than once will result in an exception being throw.
   *
   * @param trailingHeaders Trailing headers
   */
  def writeLastChunk(trailingHeaders: Map[String, String] = Map.empty[String, String]): Unit = {
    assert(writingChunks, "Must call writeFirstChunk() first")
    assert(!hasBeenWritten, "Response has ended")

    val lastChunk = new DefaultHttpChunkTrailer()
    trailingHeaders.foreach(kv => lastChunk.addHeader(kv._1, kv._2))

    val future = event.channel.write(lastChunk)
    if (!request.isKeepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }

    event.writeWebLog(this.status.code, totalChunkContentLength)
    hasBeenWritten = true
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
   * Redirection is NOT buffered. The response is immediately sent to the client.
   *
   * Calling `redirect()` more than once results in an exception being thrown because only 1 response is
   * permitted per request.
   *
   * @param url URL to which the browser will be redirected
   */
  def redirect(url: String) {
    assert(!writingChunks, "Cannot redirect after writing chunks")
    assert(!hasBeenWritten, "Response has ended")

    val closeChannel = (!request.isKeepAlive)
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND.toNetty)

    HttpResponseMessage.setDateHeader(response)
    response.setHeader(HttpHeaders.Names.LOCATION, url)

    if (!closeChannel) {
      HttpResponseMessage.setKeepAliveHeader(response, request.isKeepAlive)
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent().readableBytes())
    }

    event.writeWebLog(response.getStatus.getCode, response.getContent.readableBytes)

    val future = event.channel.write(response)
    if (closeChannel) {
      future.addListener(ChannelFutureListener.CLOSE)
    }

    hasBeenWritten = true
  }

}

object HttpResponseMessage {
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
    val dateFormatter = DateUtil.rfc1123DateFormatter
    val time = new GregorianCalendar()
    response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()))
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
   * Copy SPDY headers from request to response
   * 
   * @param request HTTP request
   * @param response HTTP response
   */
  def setSpdyHeaders(request: HttpRequestMessage, response: HttpResponse) {
    if (request.headers.contains(SpdyHttpHeaders.Names.STREAM_ID)) {
      response.setHeader(SpdyHttpHeaders.Names.STREAM_ID, request.headers(SpdyHttpHeaders.Names.STREAM_ID))
      response.setHeader(SpdyHttpHeaders.Names.PRIORITY, 0);
    }
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
  def setKeepAliveHeader(response: HttpResponse, isKeepAlive: Boolean) {
    if (isKeepAlive) {
      response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
    }
  }

  /**
   * Returns the character set from the MIME type
   *
   * For example, `UTF-8` will be extracted from `text/plain; charset=UTF-8`
   *
   * @param mimeType MIME type code.
   * @return `Charset` if specified in the MIME type, else `None`
   */
  def extractMimeTypeCharset(mimeType: String): Option[Charset] = {
    val items = mimeType.split(";")
    val csNameValue = items.find(i => i.contains("charset"))
    val charset = if (csNameValue.isDefined) {
      val csValue = csNameValue.get.replace(" ", "").replace("charset", "").replace("=", "").toUpperCase
      csValue match {
        case "UTF-8" => Some(CharsetUtil.UTF_8)
        case "US-ASCII" => Some(CharsetUtil.US_ASCII)
        case "ISO-8859-1" => Some(CharsetUtil.ISO_8859_1)
        case "UTF-16" => Some(CharsetUtil.UTF_16)
        case "UTF-16BE" => Some(CharsetUtil.UTF_16BE)
        case "UTF-16LE" => Some(CharsetUtil.UTF_16LE)
        case _ => Some(Charset.forName(csValue))
      }
    } else {
      None
    }

    charset
  }
}

/**
 * Port of Netty's HttpResponseStatus class for convenience.
 *
 * @param code HTTP response status code as per [[http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html RFC 2616]].
 */
case class HttpResponseStatus(code: Int) {

  private val nettyHttpResponseStatus = org.jboss.netty.handler.codec.http.HttpResponseStatus.valueOf(code)

  val reasonPhrase = nettyHttpResponseStatus.getReasonPhrase

  def toNetty(): org.jboss.netty.handler.codec.http.HttpResponseStatus = {
    nettyHttpResponseStatus
  }

  override def toString(): String = {
    nettyHttpResponseStatus.toString
  }
}

/**
 * Standard HTTP response status codes
 */
object HttpResponseStatus {
  /**
   * 100 Continue
   */
  val CONTINUE = new HttpResponseStatus(100)

  /**
   * 101 Switching Protocols
   */
  val SWITCHING_PROTOCOLS = new HttpResponseStatus(101)

  /**
   * 102 Processing (WebDAV, RFC2518)
   */
  val PROCESSING = new HttpResponseStatus(102)

  /**
   * 200 OK
   */
  val OK = new HttpResponseStatus(200)

  /**
   * 201 Created
   */
  val CREATED = new HttpResponseStatus(201)

  /**
   * 202 Accepted
   */
  val ACCEPTED = new HttpResponseStatus(202)

  /**
   * 203 Non-Authoritative Information (since HTTP/1.1)
   */
  val NON_AUTHORITATIVE_INFORMATION = new HttpResponseStatus(203)

  /**
   * 204 No Content
   */
  val NO_CONTENT = new HttpResponseStatus(204)

  /**
   * 205 Reset Content
   */
  val RESET_CONTENT = new HttpResponseStatus(205)

  /**
   * 206 Partial Content
   */
  val PARTIAL_CONTENT = new HttpResponseStatus(206)

  /**
   * 207 Multi-Status (WebDAV, RFC2518)
   */
  val MULTI_STATUS = new HttpResponseStatus(207)

  /**
   * 300 Multiple Choices
   */
  val MULTIPLE_CHOICES = new HttpResponseStatus(300)

  /**
   * 301 Moved Permanently
   */
  val MOVED_PERMANENTLY = new HttpResponseStatus(301)

  /**
   * 302 Found
   */
  val FOUND = new HttpResponseStatus(302)

  /**
   * 303 See Other (since HTTP/1.1)
   */
  val SEE_OTHER = new HttpResponseStatus(303)

  /**
   * 304 Not Modified
   */
  val NOT_MODIFIED = new HttpResponseStatus(304)

  /**
   * 305 Use Proxy (since HTTP/1.1)
   */
  val USE_PROXY = new HttpResponseStatus(305)

  /**
   * 307 Temporary Redirect (since HTTP/1.1)
   */
  val TEMPORARY_REDIRECT = new HttpResponseStatus(307)

  /**
   * 400 Bad Request
   */
  val BAD_REQUEST = new HttpResponseStatus(400)

  /**
   * 401 Unauthorized
   */
  val UNAUTHORIZED = new HttpResponseStatus(401)

  /**
   * 402 Payment Required
   */
  val PAYMENT_REQUIRED = new HttpResponseStatus(402)

  /**
   * 403 Forbidden
   */
  val FORBIDDEN = new HttpResponseStatus(403)

  /**
   * 404 Not Found
   */
  val NOT_FOUND = new HttpResponseStatus(404)

  /**
   * 405 Method Not Allowed
   */
  val METHOD_NOT_ALLOWED = new HttpResponseStatus(405)

  /**
   * 406 Not Acceptable
   */
  val NOT_ACCEPTABLE = new HttpResponseStatus(406)

  /**
   * 407 Proxy Authentication Required
   */
  val PROXY_AUTHENTICATION_REQUIRED = new HttpResponseStatus(407)

  /**
   * 408 Request Timeout
   */
  val REQUEST_TIMEOUT = new HttpResponseStatus(408)

  /**
   * 409 Conflict
   */
  val CONFLICT = new HttpResponseStatus(409)

  /**
   * 410 Gone
   */
  val GONE = new HttpResponseStatus(410)

  /**
   * 411 Length Required
   */
  val LENGTH_REQUIRED = new HttpResponseStatus(411)

  /**
   * 412 Precondition Failed
   */
  val PRECONDITION_FAILED = new HttpResponseStatus(412)

  /**
   * 413 Request Entity Too Large
   */
  val REQUEST_ENTITY_TOO_LARGE = new HttpResponseStatus(413)

  /**
   * 414 Request-URI Too Long
   */
  val REQUEST_URI_TOO_LONG = new HttpResponseStatus(414)

  /**
   * 415 Unsupported Media Type
   */
  val UNSUPPORTED_MEDIA_TYPE = new HttpResponseStatus(415)

  /**
   * 416 Requested Range Not Satisfiable
   */
  val REQUESTED_RANGE_NOT_SATISFIABLE = new HttpResponseStatus(416)

  /**
   * 417 Expectation Failed
   */
  val EXPECTATION_FAILED = new HttpResponseStatus(417)

  /**
   * 422 Unprocessable Entity (WebDAV, RFC4918)
   */
  val UNPROCESSABLE_ENTITY = new HttpResponseStatus(422)

  /**
   * 423 Locked (WebDAV, RFC4918)
   */
  val LOCKED = new HttpResponseStatus(423)

  /**
   * 424 Failed Dependency (WebDAV, RFC4918)
   */
  val FAILED_DEPENDENCY = new HttpResponseStatus(424)

  /**
   * 425 Unordered Collection (WebDAV, RFC3648)
   */
  val UNORDERED_COLLECTION = new HttpResponseStatus(425)

  /**
   * 426 Upgrade Required (RFC2817)
   */
  val UPGRADE_REQUIRED = new HttpResponseStatus(426)

  /**
   * 500 Internal Server Error
   */
  val INTERNAL_SERVER_ERROR = new HttpResponseStatus(500)

  /**
   * 501 Not Implemented
   */
  val NOT_IMPLEMENTED = new HttpResponseStatus(501)

  /**
   * 502 Bad Gateway
   */
  val BAD_GATEWAY = new HttpResponseStatus(502)

  /**
   * 503 Service Unavailable
   */
  val SERVICE_UNAVAILABLE = new HttpResponseStatus(503)

  /**
   * 504 Gateway Timeout
   */
  val GATEWAY_TIMEOUT = new HttpResponseStatus(504)

  /**
   * 505 HTTP Version Not Supported
   */
  val HTTP_VERSION_NOT_SUPPORTED = new HttpResponseStatus(505)

  /**
   * 506 Variant Also Negotiates (RFC2295)
   */
  val VARIANT_ALSO_NEGOTIATES = new HttpResponseStatus(506)

  /**
   * 507 Insufficient Storage (WebDAV, RFC4918)
   */
  val INSUFFICIENT_STORAGE = new HttpResponseStatus(507)

  /**
   * 510 Not Extended (RFC2774)
   */
  val NOT_EXTENDED = new HttpResponseStatus(510)

}
