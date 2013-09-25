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

import org.eclipse.jetty.npn.NextProtoNego
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
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedWriteHandler
import org.mashupbots.socko.netty.SpdyServerProvider
import org.mashupbots.socko.infrastructure.Logger

/**
 * Handler used with SPDY that performs protocol negotiation.
 *
 * Once Jetty's `NextProtoNego` returns the selected protocol, we setup the pipeline accordingly.
 *
 * Code ported from post form [[http://www.smartjava.org/content/using-spdy-and-http-transparently-using-netty Jos Dirksen]]
 *
 * @param server Web Server
 */
class ProtocolNegoitationHandler(server: WebServer) extends ChannelInboundHandlerAdapter with Logger {

  override def channelActive(ctx: ChannelHandlerContext) = {
    server.allChannels.add(ctx.channel)
  }
  
  override def channelRead(ctx: ChannelHandlerContext, e: AnyRef) = {

    val pipeline = ctx.pipeline
    val handler = pipeline.get(classOf[SslHandler])
    val provider = NextProtoNego.get(handler.engine).asInstanceOf[SpdyServerProvider]
    val selectedProtocol = provider.getSelectedProtocol
    val httpConfig = server.config.http

    // Null is returned during the negotiation process so ignore it
    if (selectedProtocol != null) {
      if (selectedProtocol.startsWith("spdy/")) {
        val version = Integer.parseInt(selectedProtocol.substring(5))

        pipeline.addLast("decoder", new SpdyFrameDecoder(version, httpConfig.maxChunkSizeInBytes,
                                                         httpConfig.maxHeaderSizeInBytes))
        pipeline.addLast("spdy_encoder", new SpdyFrameEncoder(version))
        pipeline.addLast("spdy_session_handler", new SpdySessionHandler(version, true))
        pipeline.addLast("spdy_http_encoder", new SpdyHttpEncoder(version))
        pipeline.addLast("spdy_http_decoder", new SpdyHttpDecoder(version, httpConfig.maxLengthInBytes))
        pipeline.addLast("chunkWriter", new ChunkedWriteHandler())
        pipeline.addLast("handler", new RequestHandler(server))

        // remove this handler, and process the requests as SPDY
        pipeline.remove(this)

      } else if (selectedProtocol == "http/1.1") {
        pipeline.addLast("decoder", new HttpRequestDecoder(httpConfig.maxInitialLineLength,
                                                           httpConfig.maxHeaderSizeInBytes, httpConfig.maxChunkSizeInBytes))
        if (httpConfig.aggreateChunks) {
          pipeline.addLast("chunkAggregator", new HttpObjectAggregator(httpConfig.maxLengthInBytes))
        }
        pipeline.addLast("encoder", new HttpResponseEncoder())
        pipeline.addLast("chunkWriter", new ChunkedWriteHandler())
        pipeline.addLast("handler", new RequestHandler(server))

        // remove this handler, and process the requests as HTTP
        pipeline.remove(this);

      } else {
        throw new UnsupportedOperationException("Unsupported protocol: " + selectedProtocol)
      }
    }
  }
}
