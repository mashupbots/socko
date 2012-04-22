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
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame

/**
 * Context for processing web socket frames. 
 * 
 * A [[org.mashupbots.socko.context.WsProcessingContext]] will only be dispatched to processors only after an initial 
 * [[org.mashupbots.socko.context.WsHandshakeProcessingContext]] has been successfully processed.
 *
 * @param channel Channel by which the request entered and response will be written
 * @param endPoint End point though which the original handshake request entered
 * @param wsFrame Incoming data for processing
 */
case class WsFrameProcessingContext(
  channel: Channel,
  endPoint: EndPoint,
  wsFrame: WebSocketFrame) extends ProcessingContext {

  val isText = wsFrame.isInstanceOf[TextWebSocketFrame]
  val isBinary = wsFrame.isInstanceOf[BinaryWebSocketFrame]

  /**
   * Returns the request content as a string. UTF-8 character encoding is assumed
   */
  def readStringContent(): String = {
    if (!wsFrame.isInstanceOf[TextWebSocketFrame]) {
      throw new UnsupportedOperationException("Cannot read a string from a BinaryWebSocketFrame")
    }
    wsFrame.asInstanceOf[TextWebSocketFrame].getText
  }

  /**
   * Returns the request content as a string
   *
   * @param charset Character set to use to convert data to string
   */
  def readStringContent(charset: Charset): String = {
    wsFrame.getBinaryData.toString(charset)
  }

  /**
   * Returns the request content as byte array
   */
  def readBinaryContent(): Array[Byte] = {
    wsFrame.getBinaryData.array
  }

  /**
   * Sends a text web socket frame back to the caller
   *
   * @param text Text to send to the caller
   */
  def writeText(text: String) {
    channel.write(new TextWebSocketFrame(text))
  }

  /**
   * Sends a binary web socket frame back to the caller
   *
   * @param binary Binary data to return to the caller
   */
  def writeBinaryData(binary: Array[Byte]) {
    val buf = ChannelBuffers.copiedBuffer(binary)
    channel.write(new BinaryWebSocketFrame(buf))
  }

  /**
   * Close the web socket connection
   */
  def close() {
    channel.write(new CloseWebSocketFrame())
  }
}

