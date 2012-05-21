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

import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.util.CharsetUtil
import org.mashupbots.socko.utils.WebLogEvent

/**
 * Context for processing HTTP requests.
 *
 * @param channel Channel by which the request entered and response will be written
 * @param httpRequest Incoming request for processing
 * @param config Processing configuration
 */
case class HttpRequestProcessingContext(
  channel: Channel,
  httpRequest: HttpRequest,
  config: HttpProcessingConfig) extends HttpProcessingContext {

  /**
   * HTTP End point for this request
   */
  val endPoint = EndPoint(httpRequest.getMethod.toString, HttpHeaders.getHost(httpRequest), httpRequest.getUri)

  /**
   * `True` if and only if is connection is to be kept alive and the channel should NOT be closed
   * after a response is returned.
   *
   * This flag is controlled by the existence of the keep alive HTTP header.
   * {{{
   * Connection: keep-alive
   * }}}
   */
  val isKeepAlive = HttpHeaders.isKeepAlive(httpRequest)

  /**
   * Array of accepted encoding for content compression from the HTTP header
   *
   * For example, give then header `Accept-Encoding: gzip, deflate`, then an array containing
   * `gzip` and `defalte` will be returned.
   */
  val acceptedEncodings: Array[String] = {
    val s = this.getHeader(HttpHeaders.Names.ACCEPT_ENCODING)
    if (s.isDefined) {
      s.get.replace(" ", "").split(",")
    } else {
      Array()
    }
  }

  /**
   * List of HTTP request headers
   */
  val headers = httpRequest.getHeaders

  /**
   * Returns the header value with the specified header name.  If there are
   * more than one header value for the specified header name, the first
   * value is returned.
   *
   * @return `Some(String)` or `None` if there is no such header or the header content is
   * an empty string
   */
  def getHeader(name: String): Option[String] = {
    val v = httpRequest.getHeader(name)
    if (v == null || v == "") None else Some(v)
  }

  /**
   * HTTP Version
   */
  val httpVersion = httpRequest.getProtocolVersion

  /**
   * `True` if and only if the contents of this HTTP request will be arriving in subsequent HTTPChunks
   *
   * Note that if `True`, this HTTP request will NOT have any content. The content will be coming
   * in subsequent HTTP chunks and sent for processing as `HttpChunkProcessingContext`.
   */
  val isChunked: Boolean = httpRequest.isChunked

  /**
   * `True` if and only if this is a request to upgrade to a websocket connection
   */
  def isWebSocketUpgrade: Boolean = {
    val upgrade = httpRequest.getHeader(HttpHeaders.Names.UPGRADE)
    (upgrade != null && upgrade == "websocket")
  }

  /**
   * Returns the content of this request as a string. It is assumed that content is encoded in UTF-8.
   * An empty string is returned if there is no content.
   */
  def readStringContent(): String = {
    readStringContent(CharsetUtil.UTF_8)
  }

  /**
   * Returns the content as a string. And empty string is returned if there is no content.
   *
   * @param charset Character set to use to convert data to string
   */
  def readStringContent(charset: Charset): String = {
    var content = httpRequest.getContent
    if (content.readable) content.toString(charset) else ""
  }

  /**
   * Returns the content as byte array. An empty array is returned if there is no content
   */
  def readBinaryContent(): Array[Byte] = {
    var content = httpRequest.getContent
    if (content.readable) content.array else Array.empty[Byte]
  }

  /**
   * `True` if and only if 100 continue is expected to be returned
   */
  val is100ContinueExpected = HttpHeaders.is100ContinueExpected(httpRequest)

  /**
   * Sends a 100 continue to the client
   */
  def write100Continue() {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE.toNetty)
    channel.write(response)
  }

  /**
   * Returns the If-Modified-Since header as Some(Date). None is returned if the header
   * not present or cannot be parsed
   */
  def getIfModifiedSinceHeader(): Option[Date] = {
    try {
      val ifModifiedSince = getHeader(HttpHeaders.Names.IF_MODIFIED_SINCE)
      if (ifModifiedSince.isDefined) {
        val dateFormatter = new SimpleDateFormat(HttpProcessingContext.HTTP_DATE_FORMAT, Locale.US)
        Some(dateFormatter.parse(ifModifiedSince.get))
      } else {
        None
      }
    } catch {
      case _ => None
    }
  }

  /**
   * Adds an entry to the web log
   * 
   * If you have an authenticated user, be sure to set `this.username` before writing a web log.
   * 
   * @param responseStatusCode HTTP status code
   * @param responseSize length of response content in bytes
   */
  def writeWebLog(responseStatusCode: Int, responseSize: Long) {
    if (config.webLog.isEmpty) {
      return
    }

    config.webLog.get.enqueue(WebLogEvent(
      new Date(),
      channel.getRemoteAddress,
      channel.getLocalAddress,
      username,
      httpRequest.getMethod.toString,
      httpRequest.getUri,
      responseStatusCode,
      responseSize,
      HttpHeaders.getContentLength(httpRequest),
      duration,
      httpRequest.getProtocolVersion.getText,
      getHeader(HttpHeaders.Names.USER_AGENT),
      getHeader(HttpHeaders.Names.REFERER)))
  }
}