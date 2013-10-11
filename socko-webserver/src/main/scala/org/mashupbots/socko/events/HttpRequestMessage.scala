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

import java.util.Date
import java.nio.charset.Charset

import scala.collection.JavaConversions._

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.QueryStringDecoder
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.DateUtil

/**
 * Encapsulates the all the data sent in a HTTP request; i.e. headers and content.
 */
trait HttpRequestMessage {

  /**
   * HTTP request headers
   */
  val headers: Map[String, String]

  /**
   * HTTP End point for this request
   */
  val endPoint: EndPoint

  /**
   * `True` if and only if is connection is to be kept alive and the channel should NOT be closed
   * after a response is returned.
   *
   * This flag is controlled by the existence of the keep alive HTTP header.
   * {{{
   * Connection: keep-alive
   * }}}
   */
  val isKeepAlive: Boolean

  /**
   * Array of accepted encoding for content compression from the HTTP header
   *
   * For example, give then header `Accept-Encoding: gzip, deflate`, then an array containing
   * `gzip` and `defalte` will be returned.
   */
  val acceptedEncodings: List[String]

  /**
   * HTTP version
   */
  val httpVersion: String

  /**
   * `True` if and only if 100 continue is expected to be returned
   */
  val is100ContinueExpected: Boolean

  /**
   * Returns the If-Modified-Since header as Some(Date). None is returned if the header
   * not present or cannot be parsed
   */
  val ifModifiedSince: Option[Date]

  /**
   * `True` if and only if the contents of this HTTP request will be arriving in subsequent HTTPChunks
   *
   * Note that if `True`, this HTTP request will NOT have any content. The content will be coming
   * in subsequent HTTP chunks and sent for processing as `HttpChunkEvent`.
   */
  val isChunked: Boolean

  /**
   * `True` if and only if this is a request to upgrade to a websocket connection
   */
  val isWebSocketUpgrade: Boolean

  /**
   * Content type of the body expressed as a MIME type. e.g. `text/plain`.
   */
  val contentType: String

  /**
   * Returns the length of the content from the `Content-Length` header. If not set, `0` is returned.
   */
  val contentLength: Long

  /**
   * Body of the HTTP request
   */
  val content: HttpContent
}

/**
 * HTTP request message for the current event (as opposed to an initial HTTP required to triggered a HTTP Chunk or
 * web socket event).
 *
 * @param nettyHttpRequest Netty HTTP request message
 */
case class CurrentHttpRequestMessage(nettyHttpRequest: HttpRequest) extends HttpRequestMessage {

  /**
   * HTTP request headers
   */
  lazy val headers: Map[String, String] = nettyHttpRequest.headers.map(f => (f.getKey, f.getValue)).toMap

  /**
   * HTTP End point for this request
   */
  lazy val endPoint = EndPoint(nettyHttpRequest.getMethod.toString,
    HttpHeaders.getHost(nettyHttpRequest), nettyHttpRequest.getUri)

  /**
   * `True` if and only if is connection is to be kept alive and the channel should NOT be closed
   * after a response is returned.
   *
   * This flag is controlled by the existence of the keep alive HTTP header.
   * {{{
   * Connection: keep-alive
   * }}}
   */
  lazy val isKeepAlive = HttpHeaders.isKeepAlive(nettyHttpRequest)

  /**
   * Array of accepted encoding for content compression from the HTTP header
   *
   * For example, give then header `Accept-Encoding: gzip, deflate`, then an array containing
   * `gzip` and `defalte` will be returned.
   */
  lazy val acceptedEncodings: List[String] = {
    val s = headers.get(HttpHeaders.Names.ACCEPT_ENCODING)
    if (s.isEmpty) {
      List()
    } else {
      s.get.replace(" ", "").split(",").toList
    }
  }

  /**
   * Our supported encoding; `None` if `acceptedEncodings` does not contain an encoding that we support
   */
  lazy val supportedEncoding: Option[String] = if (acceptedEncodings.contains("gzip")) {
    Some("gzip")
  } else if (acceptedEncodings.contains("deflate")) {
    Some("deflate")
  } else {
    None
  }

  /**
   * HTTP version
   */
  lazy val httpVersion = nettyHttpRequest.getProtocolVersion.toString

  /**
   * `True` if and only if 100 continue is expected to be returned
   */
  lazy val is100ContinueExpected = HttpHeaders.is100ContinueExpected(nettyHttpRequest)

  /**
   * Returns the If-Modified-Since header as Some(Date). None is returned if the header
   * not present or cannot be parsed
   */
  lazy val ifModifiedSince: Option[Date] = {
    try {
      val ifModifiedSince = headers.get(HttpHeaders.Names.IF_MODIFIED_SINCE)
      if (ifModifiedSince.isDefined) {
        val dateFormatter = DateUtil.rfc1123DateFormatter
        Some(dateFormatter.parse(ifModifiedSince.get))
      } else {
        None
      }
    } catch {
      case _: Throwable => None
    }
  }

  /**
   * `True` if and only if the contents of this HTTP request will be arriving in subsequent HTTPChunks
   *
   * Note that if `True`, this HTTP request will NOT have any content. The content will be coming
   * in subsequent HTTP chunks and sent for processing as `HttpChunkEvent`.
   */
  val isChunked: Boolean = HttpHeaders.isTransferEncodingChunked(nettyHttpRequest)

  /**
   * `True` if and only if this is a request to upgrade to a websocket connection
   * 
   * Note: Firefox sends "Connection: keep-alive, Upgrade" rather than "Connection: Upgrade"
   */
  val isWebSocketUpgrade: Boolean = {
    import HttpHeaders._
    val connection =  nettyHttpRequest.headers.get(Names.CONNECTION)
    val upgrade = nettyHttpRequest.headers.get(Names.UPGRADE)
    connection != null && """(?i)\bupgrade\b""".r.findFirstIn(connection).nonEmpty &&
    Values.WEBSOCKET.equalsIgnoreCase(upgrade)
  }

  /**
   * Content type of the body expressed as a MIME type. e.g. `text/plain`.
   */
  lazy val contentType = {
    val s = headers.get(HttpHeaders.Names.CONTENT_TYPE)
    s.getOrElse("")
  }

  /**
   * Returns the length of the content from the `Content-Length` header. If not set, `0` is returned.
   */
  lazy val contentLength = HttpHeaders.getContentLength(nettyHttpRequest)

  /**
   * Body of the HTTP request
   */
  lazy val content = nettyHttpRequest match {
    case request: FullHttpRequest =>
      DefaultHttpContent(request.content, contentType)
    case _ =>
      EmptyHttpContent
  }
}

trait HttpContent {
  def isEmpty: Boolean
  def toFormDataMap: Map[String, List[String]]
  def toString(charset: Charset): String
  def toBytes: Array[Byte]
  def toByteBuf: ByteBuf
}

object EmptyHttpContent extends HttpContent {
  def isEmpty = true
  def toFormDataMap = Map.empty[String, List[String]]
  override def toString = ""
  def toString(charset: Charset) = ""
  def toBytes = Array.empty[Byte]
  def toByteBuf = Unpooled.EMPTY_BUFFER
}

/**
 * Represents the contents or body of the HTTP request
 *
 * @param buffer Request body
 * @param contentType MIME type of the request body
 */
case class DefaultHttpContent(buffer: ByteBuf, contentType: String) extends HttpContent {

  def isEmpty = (buffer.readableBytes == 0)

  /**
   * Returns a map of the form data fields
   *
   * Empty map is returned if the content type is not `application/x-www-form-urlencoded` or if
   * there is no content.
   * 
   * The form data field name is the map's key.  The value corresponding to a key are a list values
   * for that field. Typically, there is is only 1 value for a field.
   */
  def toFormDataMap(): Map[String, List[String]] = {
    if (contentType != "application/x-www-form-urlencoded") Map.empty
    else {
      val encodedString = this.toString
      val m = new QueryStringDecoder(encodedString, false).parameters.toMap
      // Map the Java list values to Scala list
      m.map { case (key, value) => (key, value.toList) }
    }
  }

  /**
   * Returns a string representation of the content.
   *
   * The character set in the content type will be used.
   * 
   * If not supplied, ISO-8859-1 is assumed (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html section 3.7.1)
   */
  override def toString() = {
    val charset = HttpResponseMessage.extractMimeTypeCharset(contentType).getOrElse(CharsetUtil.ISO_8859_1)
    buffer.toString(charset)
  }

  /**
   * Returns a string representation of the content using the specified character set.
   *
   * @param charset Character set to use to decode the string
   */
  def toString(charset: Charset) = buffer.toString(charset)

  /**
   * Returns the contents as a byte array
   */
  def toBytes = {
    if (buffer.readableBytes > 0) buffer.array
    else Array.empty[Byte]
  }

  /**
   * Returns the contents as a Netty native channel buffer
   */
  def toByteBuf = buffer
}

/**
 * Details of the HTTP request that initiated the web socket connection or chunk transfer.
 *
 * To save space, the contents is not stored
 */
case class InitialHttpRequestMessage(
  headers: Map[String, String],
  endPoint: EndPoint,
  isKeepAlive: Boolean,
  acceptedEncodings: List[String],
  httpVersion: String,
  is100ContinueExpected: Boolean,
  ifModifiedSince: Option[Date],
  isChunked: Boolean,
  isWebSocketUpgrade: Boolean,
  contentType: String,
  contentLength: Long,
  createdOn: Date) extends HttpRequestMessage {

  def this(current: CurrentHttpRequestMessage, createdOn: Date) = this(
    current.headers,
    current.endPoint,
    current.isKeepAlive,
    current.acceptedEncodings,
    current.httpVersion,
    current.is100ContinueExpected,
    current.ifModifiedSince,
    current.isChunked,
    current.isWebSocketUpgrade,
    current.contentType,
    current.contentLength,
    createdOn)

  val content: HttpContent = EmptyHttpContent

  /**
   * Number of milliseconds from the time when the initial request was made
   */
  def duration(): Long = {
    new Date().getTime - createdOn.getTime
  }

  /**
   * Total size of chunks, in bytes, received to date.
   *
   * This is only used by HttpChunkEvent
   */
  var totalChunkContentLength: Long = 0
}

/**
 * HTTP chunk sent from client to sever
 *
 * @param nettyHttpChunk Netty representation of the HTTP Chunk
 * @param contentType Content type of the data
 */
case class HttpChunkMessage(nettyHttpChunk: io.netty.handler.codec.http.HttpContent) {

  /**
   * Returns the length of the content from the `Content-Length` header. If not set, `0` is returned.
   */
  lazy val contentLength = nettyHttpChunk.content.readableBytes

  /**
   * Flag to denote if this is the last chunk
   */
  val isLastChunk = nettyHttpChunk.isInstanceOf[LastHttpContent]

  /**
   * Trailing headers associated with the last chunk
   */
  val trailingHeaders = nettyHttpChunk match {
    case lastHttpChunk: LastHttpContent =>
      lastHttpChunk.trailingHeaders.map(e => (e.getKey, e.getValue))
    case _ =>
      Map.empty[String, String]
  }

  /**
   * Body of the HTTP chunk
   */
  val content = DefaultHttpContent(nettyHttpChunk.content, "")

}


