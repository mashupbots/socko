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

import org.eclipse.jetty.npn.NextProtoNego
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.netty.SpdyServerProvider
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.spdy.SpdyFrameDecoder
import io.netty.handler.codec.spdy.SpdyFrameEncoder
import io.netty.handler.codec.spdy.SpdyHttpDecoder
import io.netty.handler.codec.spdy.SpdyHttpEncoder
import io.netty.handler.codec.spdy.SpdySessionHandler
import io.netty.handler.codec.spdy.SpdyVersion
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.handler.timeout.IdleStateHandler
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.spdy.SpdyHttpResponseStreamIdHandler

/**
 * Handler used with SPDY that performs protocol negotiation.
 *
 * Once Jetty's `NextProtoNego` returns the selected protocol, we setup the pipeline accordingly.
 *
 * Code ported from post form [[http://www.smartjava.org/content/using-spdy-and-http-transparently-using-netty Jos Dirksen]]
 * and Netty SpdyOrHttpChooser class 
 * 
 * @param server Web Server
 */
class ProtocolNegoitationHandler(server: WebServer) extends ByteToMessageDecoder with Logger {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: java.util.List[Object]) {
    if (initPipeline(ctx)) {
      // When we reached here we can remove this handler as its now clear what protocol we want to use
      // from this point on. This will also take care of forward all messages.
      ctx.pipeline().remove(this);
    }
  }

  private def addSpdyHandlers(ctx: ChannelHandlerContext, spdyVersion: SpdyVersion) = {
    val pipeline = ctx.pipeline
    val httpConfig = server.config.http

    pipeline.addLast("spdyDecoder", new SpdyFrameDecoder(spdyVersion, httpConfig.maxChunkSizeInBytes,
      httpConfig.maxHeaderSizeInBytes))
    pipeline.addLast("spdyEncoder", new SpdyFrameEncoder(spdyVersion))
    pipeline.addLast("spdySessionHandler", new SpdySessionHandler(spdyVersion, true))
    pipeline.addLast("spdyHttpEncoder", new SpdyHttpEncoder(spdyVersion))
    pipeline.addLast("spdyHttpDecoder", new SpdyHttpDecoder(spdyVersion, httpConfig.maxLengthInBytes))
    pipeline.addLast("spdyStreamIdHandler", new SpdyHttpResponseStreamIdHandler())

    pipeline.addLast("chunkWriter", new ChunkedWriteHandler())
    if (server.config.idleConnectionTimeout.toSeconds > 0) {
      pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, server.config.idleConnectionTimeout.toSeconds.toInt))
    }

    pipeline.addLast("handler", server.handler)
  }

  private def addHttpHandlers(ctx: ChannelHandlerContext) = {
    val pipeline = ctx.pipeline
    val httpConfig = server.config.http

    pipeline.addLast("decoder", new HttpRequestDecoder(httpConfig.maxInitialLineLength,
      httpConfig.maxHeaderSizeInBytes, httpConfig.maxChunkSizeInBytes))
    if (httpConfig.aggreateChunks) {
      pipeline.addLast("chunkAggregator", new HttpObjectAggregator(httpConfig.maxLengthInBytes))
    }
    pipeline.addLast("encoder", new HttpResponseEncoder())

    pipeline.addLast("chunkWriter", new ChunkedWriteHandler())
    if (server.config.idleConnectionTimeout.toSeconds > 0) {
      pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, server.config.idleConnectionTimeout.toSeconds.toInt))
    }

    pipeline.addLast("handler", server.handler)
  }

  def initPipeline(ctx: ChannelHandlerContext): Boolean = {
    val pipeline = ctx.pipeline

    // SslHandler is needed by SPDY by design.
    val handler = pipeline.get(classOf[SslHandler])
    if (handler == null) {
      throw new IllegalStateException("SslHandler is needed for SPDY");
    }

    val provider = NextProtoNego.get(handler.engine).asInstanceOf[SpdyServerProvider]
    val selectedProtocol = provider.getSelectedProtocol

    if (selectedProtocol == null) {
      log.debug("Selected protocol unknown")
      false
    } else {
      log.debug("Selected protocol: {} ", selectedProtocol)
      selectedProtocol match {
        case "spdy/3.1" =>
          addSpdyHandlers(ctx, SpdyVersion.SPDY_3_1)
        case "spdy/3" =>
          addSpdyHandlers(ctx, SpdyVersion.SPDY_3)
        case "http/1.1" =>
          addHttpHandlers(ctx)
        case "http/1.0" =>
          addHttpHandlers(ctx)
        case _ =>
          throw new UnsupportedOperationException("Unsupported protocol: " + selectedProtocol)
      }
      true
    }

  }
}
