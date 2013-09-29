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
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.ZlibCodecFactory
import io.netty.handler.codec.compression.ZlibWrapper
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedWriteHandler
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.netty.SpdyServerProvider

/**
 * Creates a new channel pipeline for each Netty channel (network connection)
 *
 * @param server The web server instancing the pipeline
 */
class PipelineFactory(server: WebServer) extends ChannelInitializer[SocketChannel] with Logger {

  override def initChannel(ch: SocketChannel) {
    
    val pipeline = ch.pipeline
    
    if (!server.config.http.spdyEnabled) {
      
      val httpConfig = server.config.http

      if (server.sslManager.isDefined) {
        val sslEngine = server.sslManager.get.createSSLEngine()
        val ssl = new SslHandler(sslEngine);
        pipeline.addLast("ssl", ssl)
      }
            
      val httpRequestDecoder = new HttpRequestDecoder(
        httpConfig.maxInitialLineLength,
        httpConfig.maxHeaderSizeInBytes,
        httpConfig.maxChunkSizeInBytes
      )
      pipeline.addLast("decoder", httpRequestDecoder)

      if (httpConfig.aggreateChunks) {
        pipeline.addLast("chunkAggregator", new HttpObjectAggregator(httpConfig.maxLengthInBytes))
      }

      pipeline.addLast("encoder", new HttpResponseEncoder())
      pipeline.addLast("chunkWriter", new ChunkedWriteHandler())

      pipeline.addLast("handler", new RequestHandler(server))
      
    } else {
      val sslEngine = server.sslManager.get.createSSLEngine()
      val ssl = new SslHandler(sslEngine);
      pipeline.addLast("ssl", ssl)

      NextProtoNego.put(sslEngine, new SpdyServerProvider())
      NextProtoNego.debug = log.isDebugEnabled

      pipeline.addLast("pipeLineSelector", new ProtocolNegoitationHandler(server))
    }
  }
}