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

import java.text.SimpleDateFormat
import java.util.Calendar

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.handler.codec.frame.TooLongFrameException
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.util.CharsetUtil
import org.mashupbots.socko.events.HttpChunkEvent
import org.mashupbots.socko.events.HttpEventConfig
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.InitialHttpRequestMessage
import org.mashupbots.socko.events.WebSocketEventConfig
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.infrastructure.Logger

/**
 * Handles incoming HTTP messages from Netty
 *
 * @param server WebServer using this handler
 */
class RequestHandler(server: WebServer) extends SimpleChannelUpstreamHandler with Logger {

  /**
   * WebSocket handshaker used when closing web sockets
   */
  private var wsHandshaker: WebSocketServerHandshaker = null

  /**
   * Details of the initial HTTP that kicked off HTTP Chunk or WebSocket processing
   */
  private var initialHttpRequest: Option[InitialHttpRequestMessage] = None

  /**
   * HTTP processing configuration
   */
  private val httpConfig = HttpEventConfig(
    server.config.serverName,
    server.config.http.minCompressibleContentSizeInBytes,
    server.config.http.maxCompressibleContentSizeInBytes,
    server.config.http.compressibleContentTypes,
    server.webLogWriter)

  /**
   * Web socket processing configuration
   */
  private lazy val wsConfig = WebSocketEventConfig(server.config.serverName, server.webLogWriter)

  /**
   * Dispatch message to actor system for processing
   *
   * @param ctx Channel context
   * @param e Message to process
   */
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case httpRequest: HttpRequest =>
        var event = HttpRequestEvent(e.getChannel, httpRequest, httpConfig)

        log.debug("HTTP {} CHANNEL={}", event.endPoint, e.getChannel.getId)

        if (event.request.isChunked) {
          validateFirstChunk(event)
          server.routes(event)
          initialHttpRequest = Some(new InitialHttpRequestMessage(event.request, event.createdOn))
        } else if (event.request.isWebSocketUpgrade) {
          var wsctx = WebSocketHandshakeEvent(e.getChannel, httpRequest, httpConfig)
          server.routes(wsctx)
          doWebSocketHandshake(wsctx)
          initialHttpRequest = Some(new InitialHttpRequestMessage(event.request, event.createdOn))
        } else {
          server.routes(event)
        }

      case httpChunk: HttpChunk =>
        var event = HttpChunkEvent(e.getChannel, initialHttpRequest.get, httpChunk, httpConfig)
        initialHttpRequest.get.totalChunkContentLength += httpChunk.getContent.readableBytes

        log.debug("CHUNK {} CHANNEL={}", event.endPoint, e.getChannel.getId)

        server.routes(event)

        if (event.chunk.isLastChunk) {
          validateLastChunk(event)
        }

      case wsFrame: WebSocketFrame =>
        var event = WebSocketFrameEvent(e.getChannel, initialHttpRequest.get, wsFrame, wsConfig)

        log.debug("WS {} CHANNEL={}", event.endPoint, e.getChannel.getId)

        if (wsFrame.isInstanceOf[CloseWebSocketFrame]) {
          // This will also close the channel
          wsHandshaker.close(e.getChannel, wsFrame.asInstanceOf[CloseWebSocketFrame])
        } else if (wsFrame.isInstanceOf[PingWebSocketFrame]) {
          e.getChannel.write(new PongWebSocketFrame(wsFrame.getBinaryData()))
        } else {
          server.routes(event)
        }

      case _ =>
        throw new UnsupportedOperationException(e.getMessage.getClass.toString + " not supported")
    }
  }

  /**
   * Check if it is valid to process chunks and store state information.
   * 
   * An exception is thrown if invalid.
   *
   * @param event HTTP request event that is chunked
   */
  private def validateFirstChunk(event: HttpRequestEvent) {
    if (isAggreatingChunks(event.channel)) {
      if (event.request.isChunked) {
        if (initialHttpRequest.isDefined) {
          throw new IllegalStateException("New chunk started before the previous chunk ended")
        }
      }
      if (!event.request.isChunked && initialHttpRequest.isDefined) {
        throw new IllegalStateException("New request received before the previous chunk ended")
      }
    } else if (event.request.isChunked) {
      throw new IllegalStateException("Received a chunk when chunks should have been aggreated")
    }
  }

  /**
   * Check for last chunk
   */
  private def validateLastChunk(event: HttpChunkEvent) {
    if (isAggreatingChunks(event.channel)) {
      if (event.chunk.isLastChunk) {
        initialHttpRequest = None
      }
    } else {
      throw new IllegalStateException("Received a chunk when chunks should have been aggreated")
    }
  }

  /**
   * If there is an unhandled exception log and close
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    log.error("Exception caught in HttpRequestHandler", e.getCause)

    e.getCause match {
      // Cannot find route
      case ex: MatchError => writeErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, ex)
      // Request data size too big
      case ex: TooLongFrameException => writeErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, ex)
      // Websockets not supported at this route
      case ex: UnsupportedOperationException => writeErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, ex)
      // Catch all
      case ex => {
        try {
          log.debug("Error handling request", ex)
          e.getChannel().close()
        } catch {
          case ex2 => log.debug("Error closing channel", ex2)
        }
      }
    }
  }

  /**
   * Write HTTP error response and close the channel
   *
   * @param ctx Channel Event
   * @param status HTTP error status indicating the nature of the error
   * @param ex Exception
   */
  private def writeErrorResponse(ctx: ChannelHandlerContext, status: HttpResponseStatus, ex: Throwable) {
    val sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S Z")

    val msg = "Failure: %s\n\n%s\n\n%s".format(
      status.toString(),
      if (ex == null) "" else ex.getMessage,
      sf.format(Calendar.getInstance().getTime()))

    // Write HTTP Response
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
    response.setContent(ChannelBuffers.copiedBuffer(
      "Failure: " + status.toString() + "\r\n\r\n" + ex.getMessage + "\r\n",
      CharsetUtil.UTF_8))

    // Close the connection as soon as the error message is sent.
    val ch = ctx.getChannel
    if (ch.isConnected) {
      ch.write(response).addListener(ChannelFutureListener.CLOSE)
    }
  }

  /**
   * When channel open, add it to our channel group so we know which channels are
   * opened.
   *
   * Note that when a channel closes, `allChannels` automatically removes it.
   */
  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    server.allChannels.add(e.getChannel)
  }

  /**
   * Creates the web socket location - basically the same as the URL but http is replaced with ws
   *
   * @param ctx Handshake context
   */
  private def createWebSocketLocation(ctx: WebSocketHandshakeEvent): String = {
    val sb = new StringBuilder
    sb.append(if (isSSLConnection(ctx.channel)) "wss" else "ws")
    sb.append("://")
    sb.append(ctx.request.headers.get(HttpHeaders.Names.HOST))
    sb.append(ctx.endPoint.uri)
    sb.toString
  }

  /**
   * Performs web socket handshake
   *
   * @param event Handshake event
   */
  private def doWebSocketHandshake(event: WebSocketHandshakeEvent): Unit = {
    if (!event.isAuthorized) {
      throw new UnsupportedOperationException("Websocket not supported at this end point")
    }

    val wsFactory = new WebSocketServerHandshakerFactory(
      createWebSocketLocation(event),
      if (event.authorizedSubprotocols == "") null else event.authorizedSubprotocols,
      false,
      event.maxFrameSize)
    wsHandshaker = wsFactory.newHandshaker(event.nettyHttpRequest)
    if (wsHandshaker == null) {
      wsFactory.sendUnsupportedWebSocketVersionResponse(event.channel)
      event.writeWebLog(HttpResponseStatus.UPGRADE_REQUIRED.getCode, 0)
    } else {
      wsHandshaker.handshake(event.channel, event.nettyHttpRequest)
      event.writeWebLog(HttpResponseStatus.SWITCHING_PROTOCOLS.getCode, 0)
    }
  }

  /**
   * Check if SSL is being used
   */
  private def isSSLConnection(channel: Channel): Boolean = {
    (channel.getPipeline().get(classOf[SslHandler]) != null)
  }

  /**
   * Check if this channel is aggregating chunks
   */
  private def isAggreatingChunks(channel: Channel): Boolean = {
    (channel.getPipeline().get(classOf[HttpChunkAggregator]) != null)
  }

}