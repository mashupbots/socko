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

import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executors

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion
import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseDecoder
import org.mashupbots.socko.infrastructure.Logger

/**
 * Encapsulates a web socket client for use in testing
 */
class TestWebSocketClient(url: String) extends Logger {

  val bootstrap = new ClientBootstrap(
    new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()))

  val uri = new URI(url)

  val handshaker = new WebSocketClientHandshakerFactory().newHandshaker(
    new URI(url), WebSocketVersion.V13, null, false, null)

  var ch: Channel = null
  val channelData = new ChannelData()
  val monitor = new AnyRef()

  bootstrap.setPipelineFactory(new PipeLineFactory(handshaker, channelData, monitor))

  /**
   * Connect to the server. This is a blocking call.
   */
  def connect() {
    if (this.isConnected) {
      return
    }

    // Initialize connection status
    channelData.isConnected = None

    log.debug("WebSocket Client connecting")
    val future = bootstrap.connect(new InetSocketAddress(uri.getHost, uri.getPort))
    future.awaitUninterruptibly()

    ch = future.getChannel()
    handshaker.handshake(ch)

    // Wait until connected
    monitor.synchronized {
      while (channelData.isConnected.isEmpty) {
        monitor.wait()
      }
    }
  }

  /**
   * Send text to the server and wait for response
   *
   * @param content Content to send
   * @param waitForResponse Block until a response has been received
   */
  def send(content: String, waitForResponse: Boolean = false) {
    channelData.hasReplied = false
    ch.write(new TextWebSocketFrame(content))
    if (waitForResponse) {
      monitor.synchronized {
        while (!channelData.hasReplied) {
          monitor.wait()
        }
      }
    }
  }

  /**
   * Disconnect from the server
   */
  def disconnect() {
    ch.write(new CloseWebSocketFrame())

    ch.getCloseFuture().awaitUninterruptibly()
    bootstrap.releaseExternalResources();
  }

  /**
   * Text that has been received
   */
  def getReceivedText(): String = {
    channelData.textBuffer.toString
  }

  /**
   * Flag to indicate if the web service connection has been made
   */
  def isConnected(): Boolean = {
    if (channelData.isConnected.isEmpty) false else channelData.isConnected.get
  }

  /**
   * Creates a new pipeline for every connection
   */
  class PipeLineFactory(
    handshaker: WebSocketClientHandshaker,
    channelData: ChannelData,
    connectionMonitor: AnyRef) extends ChannelPipelineFactory {
    def getPipeline: ChannelPipeline = {
      val newPipeline = Channels.pipeline()
      newPipeline.addLast("decoder", new HttpResponseDecoder())
      newPipeline.addLast("encoder", new HttpRequestEncoder())
      newPipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker, channelData, connectionMonitor))
      newPipeline
    }
  }

  /**
   * Handler for processing incoming data
   */
  class WebSocketClientHandler(
    handshaker: WebSocketClientHandshaker,
    channelData: ChannelData,
    connectionMonitor: AnyRef) extends SimpleChannelUpstreamHandler with Logger {

    override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      log.debug("WebSocket Client disconnected!");
      channelData.isConnected = Some(false)
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      val ch = ctx.getChannel()
      if (!handshaker.isHandshakeComplete()) {
        try {
          handshaker.finishHandshake(ch, e.getMessage.asInstanceOf[HttpResponse])
          log.debug("WebSocket Client connected!")
          channelData.isConnected = Some(true)
        } catch {
          case _ => {
            log.debug("Error connecting to Web Socket Server")
            channelData.isConnected = Some(false)
          }
        }
      } else {
        val frame = e.getMessage()
        if (frame.isInstanceOf[TextWebSocketFrame]) {
          val textFrame = frame.asInstanceOf[TextWebSocketFrame]
          channelData.textBuffer.append(textFrame.getText)
          channelData.textBuffer.append("\n")
          log.debug("WebSocket Client received message: " + textFrame.getText)
          channelData.hasReplied = true
        } else if (frame.isInstanceOf[PongWebSocketFrame]) {
          log.debug("WebSocket Client received pong")
        } else if (frame.isInstanceOf[CloseWebSocketFrame]) {
          log.debug("WebSocket Client received closing")
          ch.close()
        }
      }

      // Notify monitor that we have connected
      connectionMonitor.synchronized {
        connectionMonitor.notifyAll()
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      val t = e.getCause();
      t.printStackTrace();
      e.getChannel().close();
    }
  }

  /**
   * Data associated with a specific channel
   */
  class ChannelData() {
    var isConnected: Option[Boolean] = None
    var hasReplied = false
    val textBuffer = new StringBuilder()
  }
}