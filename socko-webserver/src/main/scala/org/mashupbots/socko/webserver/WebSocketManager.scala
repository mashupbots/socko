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
package org.mashupbots.socko.webserver

import org.mashupbots.socko.events.WebSocketEventConfig

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.group.ChannelMatcher
import io.netty.channel.group.ChannelMatchers
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.util.concurrent.GlobalEventExecutor

import scala.collection.JavaConversions._

/**
 * Manages all web socket connections
 *
 * Acts as a wrapper for Netty's channel group.
 *
 * @param name Name to call this web socket manager. Netty channel group will  use this name.
 */
class WebSocketManager(val name: String) {

  /**
   * Collection of channels that are currently being used
   */
  val allWebSocketChannels = new DefaultChannelGroup(name, GlobalEventExecutor.INSTANCE)

  /**
   * Adds the specified Netty channel for management.  For internal package use only.
   *
   * @param channel Netty channel to add
   */
  protected[webserver] def addChannel(channel: Channel) = {
    allWebSocketChannels.add(channel);
  }

  /**
   * Disconnects the specified web socket and removes it from the group
   *
   * @param websocketId ID of web socket to remove
   */
  def closeChannel(websocketId: String) = {
    allWebSocketChannels.disconnect(WebSocketIdChannelMatcher(List(websocketId)))
  }

  /**
   * Disconnects the specified web sockets and removes it from the group
   *
   * @param websocketIds IDs of web sockets to remove
   */
  def closeChannel(websocketIds: Iterable[String]) = {
    allWebSocketChannels.disconnect(WebSocketIdChannelMatcher(websocketIds))
  }

  /**
   * Disconnects all web sockets
   */
  def closeAllChannels() = {
    allWebSocketChannels.disconnect(ChannelMatchers.all())
  }

  /**
   * Sends a web socket text message to the specified web socket
   */
  def writeText(text: String, websocketId: String) {
    writeText(text, List(websocketId))
  }

  /**
   * Sends a web socket text message to the specified web sockets
   */
  def writeText(text: String, websocketIds: Iterable[String]) {
    allWebSocketChannels.write(new TextWebSocketFrame(text), WebSocketIdChannelMatcher(websocketIds))
    allWebSocketChannels.flush
  }

  /**
   * Broadcasts a web socket text message to all web socket
   */
  def writeText(text: String) {
    allWebSocketChannels.write(new TextWebSocketFrame(text), ChannelMatchers.all())
    allWebSocketChannels.flush
  }

  /**
   * Sends a web socket text message to the specified web socket
   */
  def writeBinary(bytes: Array[Byte], websocketId: String) {
    writeBinary(bytes, List(websocketId))
  }

  /**
   * Sends a web socket text message to the specified web sockets
   */
  def writeBinary(bytes: Array[Byte], websocketIds: Iterable[String]) {
    allWebSocketChannels.write(new BinaryWebSocketFrame(Unpooled.buffer(bytes.length).writeBytes(bytes)), WebSocketIdChannelMatcher(websocketIds))
    allWebSocketChannels.flush
  }

  /**
   * Broadcasts a web socket text message to all web socket
   */
  def writeBinary(bytes: Array[Byte]) {
    allWebSocketChannels.write(new BinaryWebSocketFrame(Unpooled.buffer(bytes.length).writeBytes(bytes)), ChannelMatchers.all())
    allWebSocketChannels.flush
  }

  /**
   * Checks if the specified web socket id is still connected
   * 
   * @param websocketId Id of web socket to check if it is still connected
   * @returns True if connected, False if the channel has been closed.
   */
  def isConnected(websocketId: String): Boolean = {
    allWebSocketChannels.iterator().exists(c => c.attr(WebSocketEventConfig.websocketIdKey).get() == websocketId)
  }
  
  /**
   * Matcher for web socket id to use with channel groups
   */
  case class WebSocketIdChannelMatcher(websocketIds: Iterable[String]) extends ChannelMatcher {
    assert(websocketIds != null)

    def this(websocketId: String) = this(Seq(websocketId))

    def matches(channel: io.netty.channel.Channel): Boolean = {
      val channelId = channel.attr(WebSocketEventConfig.websocketIdKey).get()
      websocketIds.exists(id => id == channelId)
    }
  }
}