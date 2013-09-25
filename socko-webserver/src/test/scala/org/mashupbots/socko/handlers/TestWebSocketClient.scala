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

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestEncoder
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseDecoder

import org.mashupbots.socko.infrastructure.Logger

/**
 * Encapsulates a web socket client for use in testing
 */
class TestWebSocketClient(url: String, subprotocols: String = null) extends Logger {

  val group = new NioEventLoopGroup
  val bootstrap = new Bootstrap()
    .group(group)
    .channel(classOf[NioSocketChannel])

  val uri = new URI(url)

  val handshaker = WebSocketClientHandshakerFactory.newHandshaker(
    new URI(url), WebSocketVersion.V13, subprotocols, false, null)

  var ch: Channel = null
  val channelData = new ChannelData()
  val monitor = new AnyRef()

  bootstrap.handler(new PipeLineFactory(handshaker, channelData, monitor))

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

    ch = future.channel
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
    ch.writeAndFlush(new TextWebSocketFrame(content))
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
    ch.writeAndFlush(new CloseWebSocketFrame)
    ch.closeFuture.awaitUninterruptibly
    group.shutdownGracefully()
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
    connectionMonitor: AnyRef
  ) extends ChannelInitializer[SocketChannel] {

    def initChannel(channel: SocketChannel) = {
      val pipeline = channel.pipeline
      pipeline.addLast("decoder", new HttpResponseDecoder())
      pipeline.addLast("encoder", new HttpRequestEncoder())
      pipeline.addLast("aggregator", new HttpObjectAggregator(8196))
      pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker, channelData, connectionMonitor))
    }
  }

  /**
   * Handler for processing incoming data
   */
  class WebSocketClientHandler(
    handshaker: WebSocketClientHandshaker,
    channelData: ChannelData,
    connectionMonitor: AnyRef
  ) extends ChannelInboundHandlerAdapter with Logger {

    override def channelInactive(ctx: ChannelHandlerContext) = {
      log.debug("WebSocket Client disconnected!");
      channelData.isConnected = Some(false)
      
      // Notify monitor that we have disconnected
      connectionMonitor.synchronized {
        connectionMonitor.notifyAll()
      }      
    }

    override def channelRead(ctx: ChannelHandlerContext, e: AnyRef) {
      e match {
        case response: FullHttpResponse =>
          if (!handshaker.isHandshakeComplete()) {
            try {
              handshaker.finishHandshake(ctx.channel, e.asInstanceOf[FullHttpResponse])
              log.debug("WebSocket Client connected!")
              channelData.isConnected = Some(true)
            } catch {
              case ex: Throwable => {
                  log.debug("Error connecting to Web Socket Server", ex)
                  channelData.isConnected = Some(false)
                }
            }
          }
          
        case frame: TextWebSocketFrame =>
          val textFrame = frame.asInstanceOf[TextWebSocketFrame]
          channelData.textBuffer.append(textFrame.text)
          channelData.textBuffer.append("\n")
          log.debug("WebSocket Client received message: " + textFrame.text)
          channelData.hasReplied = true
          
        case frame: PongWebSocketFrame =>
          log.debug("WebSocket Client received pong")
          
        case frame: CloseWebSocketFrame =>
          log.debug("WebSocket Client received closing")
          ch.close()
      }

      // Notify monitor that we have connected
      connectionMonitor.synchronized {
        connectionMonitor.notifyAll()
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {
      e.getCause.printStackTrace
      ctx.channel.close
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