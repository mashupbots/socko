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

import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import org.mashupbots.socko.infrastructure.WebLogEvent
import java.util.Date
import org.jboss.netty.handler.codec.http.HttpHeaders

/**
 * Event fired when a web socket text or binary frame is received.
 *
 * A [[org.mashupbots.socko.events.WebSocketFrameEvent]] will only be fired after an initial
 * [[org.mashupbots.socko.events.WebSocketFrameHandshakeEvent]] has been successfully processed.
 *
 * @param channel Channel by which the request entered and response will be written
 * @param initialHttpRequest The initial HTTP request
 * @param wsFrame Incoming data for processing
 * @param config Web Socket configuration
 */
case class WebSocketFrameEvent(
  channel: Channel,
  initialHttpRequest: InitialHttpRequestMessage,
  wsFrame: WebSocketFrame,
  config: WebSocketEventConfig) extends SockoEvent {

  /**
   * HTTP end point used by this chunk
   */
  val endPoint = initialHttpRequest.endPoint

  /**
   * Indicates a text frame
   */
  val isText = wsFrame.isInstanceOf[TextWebSocketFrame]

  /**
   * Indicates a binary frame
   */
  val isBinary = wsFrame.isInstanceOf[BinaryWebSocketFrame]

  /**
   * Web socket version
   */
  val protocolVersion = initialHttpRequest.httpVersion + ":" +
    initialHttpRequest.headers.get(HttpHeaders.Names.SEC_WEBSOCKET_VERSION)

  /**
   * Returns the request content as a string. UTF-8 character encoding is assumed
   */
  def readText(): String = {
    if (!wsFrame.isInstanceOf[TextWebSocketFrame]) {
      throw new UnsupportedOperationException("Cannot read a string from a BinaryWebSocketFrame")
    }
    wsFrame.asInstanceOf[TextWebSocketFrame].getText
  }

  /**
   * Sends a text web socket frame back to the client
   *
   * @param text Text to send to the client
   */
  def writeText(text: String) {
    channel.write(new TextWebSocketFrame(text))
  }

  /**
   * Returns the request content as byte array
   */
  def readBinary(): Array[Byte] = {
    wsFrame.getBinaryData.array
  }

  /**
   * Sends a binary web socket frame back to the client
   *
   * @param binary Binary data to return to the client
   */
  def writeBinary(binary: Array[Byte]) {
    val buf = ChannelBuffers.copiedBuffer(binary)
    channel.write(new BinaryWebSocketFrame(buf))
  }

  /**
   * Close the web socket connection
   */
  def close() {
    channel.write(new CloseWebSocketFrame())
  }

  /**
   * Adds an entry to the web log.
   *
   * Web logs were designed for request/response style interaction and not the duplex communication channel of
   * websockets.
   *
   * By default, Socko does not write web logs for websocket frames. This is because Socko does not know the context
   * of your frames. However, you can write web logs by calling this method from your route or actor processor.
   *
   * If you have authenticated the user, you can set it in `this.username`.
   *
   * @param method Can be used to describe the operation or the message type. No spaces allowed.
   * @param uri Can be used to provide context. Querystring is also permissible.
   * @param requestSize Length of request frame. Set to 0 if none.
   * @param responseStatusCode Status code
   * @param responseSize Length of response frame in bytes. Set to 0 if none.
   */
  def writeWebLog(method: String, uri: String, requestSize: Long, responseStatusCode: Int, responseSize: Long) {
    if (config.webLogWriter.isEmpty) {
      return
    }

    config.webLogWriter.get ! WebLogEvent(
      this.createdOn,
      config.serverName,
      channel.getId,
      channel.getRemoteAddress,
      channel.getLocalAddress,
      username,
      method,
      uri,
      requestSize,
      responseStatusCode,
      responseSize,
      duration,
      protocolVersion,
      None,
      None)
  }

}

