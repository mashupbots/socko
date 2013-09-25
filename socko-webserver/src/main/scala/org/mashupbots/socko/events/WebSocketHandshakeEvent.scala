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

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest

import org.mashupbots.socko.infrastructure.WebLogEvent

/**
 * Event fired when performing a web socket handshake to upgrade a HTTP connection to a web socket connection.
 *
 * Socko requires this event be processed in your route and NOT passed to actors.
 * The only action that needs to be taken is to call `event.authorize()`.
 *
 * {{{
 * val routes = Routes({
 *   case event @ Path("/snoop/websocket/") => event match {
 *     case event: WebSocketHandshakeEvent => {
 *       event.authorize()
 *     }
 *     case event: WebSocketFrameEvent => {
 *       myActorSystem.actorOf(Props[MyWebSocketFrameProcessor], name) ! event
 *     }
 *   }
 * })
 * }}}
 *
 * Calling `event.authorize()` authorizes Socko to perform all the necessary handshaking. If not called,
 * Socko will reject the handshake and web sockets processing will be aborted.
 *
 * `event.authorize()` is a security measure to ensure that upgrades to web socket connections is only performed at
 * explicit routes.
 *
 * @param channel Channel by which the request entered and response will be written
 * @param nettyHttpRequest HTTP request associated with the upgrade to web sockets connection
 * @param config Processing configuration
 */
case class WebSocketHandshakeEvent(
  context: ChannelHandlerContext,
  nettyHttpRequest: FullHttpRequest,
  config: HttpEventConfig) extends HttpEvent {

  /**
   * Incoming HTTP request
   */
  val request = CurrentHttpRequestMessage(nettyHttpRequest)

  /**
   * Always s set to `null` because no response is available for handshakes. Let the handshaker do the work for you.
   */
  val response = null

  /**
   * HTTP end point
   */
  val endPoint = request.endPoint

  private var _isAuthorized: Boolean = false

  private var _authorizedSubprotocols: String = ""

  private var _maxFrameSize: Int = 0

  private var _onComplete: Option[(WebSocketHandshakeEvent) => Unit] = None

  /**
   * Authorize this web socket handshake to proceed
   *
   * @param subprotocol Comma separated list of supported protocols. e.g. `chat, stomp`. Specified empty string to
   *   not support sub protocols (this is the default).
   * @param maxFrameSize Maximum size of web socket frames. Defaults to 100K.
   * @param onComplete Optional callback executed when the handshake is successfully completed. You can use this 
   *   callback to register the web socket client as being ready to receive data. 
   */
  def authorize(subprotocols: String = "", maxFrameSize: Int = 102400,
    onComplete: Option[(WebSocketHandshakeEvent) => Unit] = None) {
    _isAuthorized = true
    _authorizedSubprotocols = if (subprotocols == null) "" else subprotocols
    _maxFrameSize = maxFrameSize
    _onComplete = onComplete
  }

  /**
   * Indicates if this web socket handshake is authorized or not
   */
  def isAuthorized: Boolean = {
    _isAuthorized
  }

  /**
   * Comma separated list of supported protocols. e.g. `chat, stomp`
   */
  def authorizedSubprotocols: String = {
    _authorizedSubprotocols
  }

  /**
   * Maximum size of frames for this web socket connection in bytes.
   */
  def maxFrameSize: Int = {
    _maxFrameSize
  }
  
  /**
   * Callback for when the handshake as completed
   */
  def onComplete: Option[(WebSocketHandshakeEvent) => Unit] = {
    _onComplete
  }

  /**
   * Adds an entry to the web log
   *
   * @param responseStatusCode HTTP status code
   * @param responseSize length of response content in bytes
   */
  def writeWebLog(responseStatusCode: Int, responseSize: Long) {
    if (config.webLogWriter.isEmpty) {
      return
    }

    config.webLogWriter.get ! WebLogEvent(
      this.createdOn,
      config.serverName,
      context.name,
      context.channel.remoteAddress,
      context.channel.localAddress,
      username,
      request.endPoint.method,
      request.endPoint.uri,
      request.contentLength,
      responseStatusCode,
      responseSize,
      duration,
      request.httpVersion,
      request.headers.get(HttpHeaders.Names.USER_AGENT),
      request.headers.get(HttpHeaders.Names.REFERER))
  }
}