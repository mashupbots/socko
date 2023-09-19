//
// Copyright 2012-2013 Vibul Imtarnasan, David Bolton and Socko contributors.
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
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.TooLongFrameException
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.ssl.SslHandler
import org.mashupbots.socko.events.HttpChunkEvent
import org.mashupbots.socko.events.HttpEventConfig
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.InitialHttpRequestMessage
import org.mashupbots.socko.events.SockoEvent
import org.mashupbots.socko.events.WebSocketEventConfig
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.events.HttpLastChunkEvent
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleState
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame

/**
 * Handles incoming HTTP messages from Netty
 *
 * @param server WebServer using this handler
 * @param routes Routes to request processing actors
 */
class RequestHandler(server: WebServer, routes: PartialFunction[SockoEvent, Unit]) extends ChannelInboundHandlerAdapter with Logger {

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
   * Handle idle state timeouts so we can close idle connections
   */
  override def userEventTriggered(ctx: ChannelHandlerContext, evt: Object) = {
    evt match {
      case idleStateEvent: IdleStateEvent =>
        if (idleStateEvent.state() == IdleState.ALL_IDLE) {
          ctx.close()
        }
      case _ =>
      // Ignore
    }
  }

  /**
   * Dispatch message to actor system for processing
   *
   * @param ctx Channel context
   * @param e Message to process
   */
  override def channelRead(ctx: ChannelHandlerContext, e: AnyRef) {
    e match {
      case httpRequest: FullHttpRequest =>
        val event = HttpRequestEvent(ctx, httpRequest, httpConfig)

        log.debug("HTTP FULL REQUEST {} CHANNEL={} {}", event.endPoint, ctx.name, "")

        if (event.request.isWebSocketUpgrade) {
          val wsctx = WebSocketHandshakeEvent(ctx, httpRequest, httpConfig)
          routes(wsctx)
          doWebSocketHandshake(wsctx)
          initialHttpRequest = Some(new InitialHttpRequestMessage(event.request, event.createdOn))
        } else {
          routes(event)
        }

      case httpRequest: HttpRequest =>
        val event = HttpRequestEvent(ctx, httpRequest, httpConfig)

        log.debug("HTTP REQUEST {} CHANNEL={} {}", event.endPoint, ctx.name, "")

        validateFirstChunk(event)
        routes(event)
        initialHttpRequest = Some(new InitialHttpRequestMessage(event.request, event.createdOn))

      case httpLastChunk: LastHttpContent =>
        val event = HttpLastChunkEvent(ctx, initialHttpRequest.get, httpLastChunk, httpConfig)

        log.debug("HTTP LAST CHUNK {} CHANNEL={} {}", event.endPoint, ctx.name, "")

        routes(event)
        validateLastChunk(event)

      case httpChunk: HttpContent =>
        val event = HttpChunkEvent(ctx, initialHttpRequest.get, httpChunk, httpConfig)
        initialHttpRequest.get.totalChunkContentLength += httpChunk.content.readableBytes

        log.debug("HTTP CHUNK {} CHANNEL={} {}", event.endPoint, ctx.name, "")

        routes(event)

      case wsFrame: WebSocketFrame =>
        val event = WebSocketFrameEvent(ctx, initialHttpRequest.get, wsFrame, wsConfig)

        log.debug("WS {} CHANNEL={} {}", event.endPoint, ctx.name, "")

        wsFrame match {
          case f: TextWebSocketFrame =>
            routes(event)

          case f: BinaryWebSocketFrame =>
            routes(event)
          
          case f: ContinuationWebSocketFrame =>
            routes(event)
            
          case f: CloseWebSocketFrame =>
          	// This will also close the channel
          	wsHandshaker.close(ctx.channel, f)
          	
          case f: PingWebSocketFrame =>
            // Send pong frame
          	ctx.writeAndFlush(new PongWebSocketFrame(f.isFinalFragment, f.rsv, f.content))
          	
          case f: PongWebSocketFrame =>
            // Ignore
          	wsFrame.release
          	
          case _ =>
            throw new UnsupportedOperationException("Web socket frame not supported: " + wsFrame.getClass.getName) 
        }       

      case _ =>
        throw new UnsupportedOperationException(e.getClass.toString + " not supported")
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
    if (isAggreatingChunks(event.context.channel)) {
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
  private def validateLastChunk(event: HttpLastChunkEvent) {
    if (isAggreatingChunks(event.context.channel)) {
      initialHttpRequest = None
    } else {
      throw new IllegalStateException("Received a chunk when chunks should have been aggreated")
    }
  }

  /**
   * If there is an unhandled exception log and close
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {
    log.error("Exception caught in HttpRequestHandler", e)

    e match {
      // Cannot find route
      case ex: MatchError => writeErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, ex)
      // Request data size too big
      case ex: TooLongFrameException => writeErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, ex)
      // Websockets not supported at this route
      case ex: UnsupportedOperationException => writeErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, ex)
      // Websockets handshake error
      case ex: WebSocketHandshakeException => writeErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, ex)
      // Catch all
      case ex => {
        try {
          log.debug("Error handling request", ex)
          ctx.channel.close
        } catch {
          case ex2: Throwable => log.debug("Error closing channel", ex2)
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
    val bytes = s"Failure: ${status}\r\n\r\n${ex.getMessage}\r\n".getBytes(CharsetUtil.UTF_8)
    val content = Unpooled.wrappedBuffer(bytes)
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content)
    response.headers.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")

    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
  }

  /**
   * When channel open, add it to our channel group so we know which channels are
   * opened.
   *
   * Note that when a channel closes, `allChannels` automatically removes it.
   */
  override def channelActive(ctx: ChannelHandlerContext) {
    server.allChannels.add(ctx.channel)
  }

  /**
   * Creates the web socket location - basically the same as the URL but http is replaced with ws
   *
   * @param ctx Handshake context
   */
  private def createWebSocketLocation(e: WebSocketHandshakeEvent): String = {
    val sb = new StringBuilder
    sb.append(if (isSSLConnection(e.context.channel)) "wss" else "ws")
    sb.append("://")
    sb.append(e.request.headers.get(HttpHeaders.Names.HOST))
    sb.append(e.endPoint.uri)
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
      true,
      event.maxFrameSize)
    wsHandshaker = wsFactory.newHandshaker(event.nettyHttpRequest)
    if (wsHandshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(event.context.channel)
      event.writeWebLog(HttpResponseStatus.UPGRADE_REQUIRED.code, 0)
    } else {
      val id = event.webSocketId
      val future = wsHandshaker.handshake(event.context.channel, event.nettyHttpRequest)
      event.writeWebLog(HttpResponseStatus.SWITCHING_PROTOCOLS.code, 0)

      // Register websockets with the manager
      server.webSocketConnections.add(event.context.channel)

      // Callback on complete AFTER data sent to the client
      if (event.onComplete.isDefined) {
        class OnCompleteListender extends ChannelFutureListener {
          def operationComplete(future: ChannelFuture) {
            event.onComplete.get(id)
          }
        }
        future.addListener(new OnCompleteListender())
      }

      // Callback on close - after the web socket connection (netty channel) is closed
      if (event.onClose.isDefined) {
        class OnCloseListender extends ChannelFutureListener {
          def operationComplete(future: ChannelFuture) {
            event.onClose.get(id)
          }
        }
        event.context.channel.closeFuture.addListener(new OnCloseListender())
      }
    }
  }

  /**
   * Check if SSL is being used
   */
  private def isSSLConnection(channel: Channel): Boolean = {
    (channel.pipeline.get(classOf[SslHandler]) != null)
  }

  /**
   * Check if this channel is aggregating chunks
   */
  private def isAggreatingChunks(channel: Channel): Boolean = {
    (channel.pipeline.get(classOf[HttpObjectAggregator]) != null)
  }

}