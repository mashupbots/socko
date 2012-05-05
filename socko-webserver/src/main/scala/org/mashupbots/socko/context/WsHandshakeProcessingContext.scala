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

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.util.CharsetUtil

/**
 * Context for processing web socket handshakes.
 *
 * Socko requires that this context be processed in your route and NOT passed to processor actors.
 * The only action that needs to be taken is to set `isAllowed` to `True`.
 *
 * {{{
 * val routes = Routes({
 *   case ctx @ Path("/snoop/websocket/") => ctx match {
 *     case ctx: WsHandshakeProcessingContext => {
 *       val hctx = ctx.asInstanceOf[WsHandshakeProcessingContext]
 *       hctx.isAllowed = true
 *     }
 *     case ctx: WsProcessingContext => {
 *       myActorSystem.actorOf(Props[MyWebSocketFrameProcessor], name) ! ctx
 *     }
 *   }
 * })
 * }}}
 *
 * If `isAllowed` is set to `True`, Socko will perform all the necessary handshaking. If false,
 * the default value, Socko will reject the handshake and web sockets processing will be aborted.
 *
 * `isAllowed` has been added as a security measure to make sure that upgrade to web sockets is only performed at
 * explicit routes.
 *
 * @param channel Channel by which the request entered and response will be written
 * @param httpRequest HTTP request associated with the upgrade to web sockets connection
 * @param config Processing configuration
 */
case class WsHandshakeProcessingContext(
  channel: Channel,
  httpRequest: HttpRequest,
  config: HttpProcessingConfig) extends HttpProcessingContext {

  /**
   * HTTP end point
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
   * HTTP headers
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
   * Flag to be set to `True` by the route to indicate if the upgrade to a web socket connection is permitted or not
   */
  var isAllowed: Boolean = false

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

}