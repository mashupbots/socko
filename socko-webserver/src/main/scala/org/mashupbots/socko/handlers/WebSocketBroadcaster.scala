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
package org.mashupbots.socko.handlers

import io.netty.buffer.Unpooled
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
//import org.mashupbots.socko.events.WebSocketHandshakeEvent

import akka.actor.Actor
import io.netty.util.concurrent.ImmediateEventExecutor
import org.mashupbots.socko.events.WebSocketHandshakeEvent

/**
 * Broadcasts a message to registered web socket connections.
 * 
 * Original idea from [[http://www.cakesolutions.net/teamblogs/2012/05/10/web-socket-push-notifications-in-akka-netty-and-socko/ Jan Machacek]]
 * 
 * Usage:
 *  1. Create WebSocketBroadcaster
 *  2. During web socket handshake, send [[org.mashupbots.socko.handlers.WebSocketBroadcasterRegistration]] message
 *     to the actor created in step #1.
 *  3. To broadcast, send [[org.mashupbots.socko.handlers.WebSocketBroadcastText]] or 
 *     [[org.mashupbots.socko.handlers.WebSocketBroadcastBinary]] to the actor created in step #1.
 * 
 * There is no need to de-register a web socket upon disconnection.  Netty does this for us automatically. 
 * 
 * For more information, see the example `ChatApp`.
 */
class WebSocketBroadcaster extends Actor {
  private val socketConnections = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE)

  def receive = {
    case WebSocketBroadcastText(text) =>
      socketConnections.write(new TextWebSocketFrame(text))
      socketConnections.flush
    case WebSocketBroadcastBinary(bytes) =>
      socketConnections.write(new BinaryWebSocketFrame(Unpooled.buffer(bytes.length).writeBytes(bytes)))
      socketConnections.flush
    case WebSocketBroadcasterRegistration(event) =>
      socketConnections.add(event.context.channel)
  }
}

/**
 * Message sent to [[org.mashupbots.socko.handlers.WebSocketBroadcaster]] during the web socket handshake 
 * in order to register the web socket connection to receive broadcast messages
 */
case class WebSocketBroadcasterRegistration(context: WebSocketHandshakeEvent)

/**
 * Message sent to [[org.mashupbots.socko.handlers.WebSocketBroadcaster]] to broadcast a web socket text frame
 * to all registered web socket connection.
 */
case class WebSocketBroadcastText(text: String)

/**
 * Message sent to [[org.mashupbots.socko.handlers.WebSocketBroadcaster]] to broadcast a web socket binary frame
 * to all registered web socket connection.
 */
case class WebSocketBroadcastBinary(bytes: Array[Byte])
