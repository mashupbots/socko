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
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.Channels
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.netty.SpdyServerProvider

/**
 * Creates a new channel pipeline for each Netty channel (network connection)
 *
 * @param server The web server instancing the pipeline
 */
class PipelineFactory(server: WebServer) extends ChannelPipelineFactory with Logger {

  /**
   * Returns a new channel pipeline instance to handle our channel
   */
  def getPipeline(): ChannelPipeline = {
    if (server.config.http.spdyEnabled) getSpdyPipeline() else getHttpPipeline()
  }

  private def getHttpPipeline(): ChannelPipeline = {
    val newPipeline = Channels.pipeline()
    val httpConfig = server.config.http

    if (server.sslManager.isDefined) {
      val sslEngine = server.sslManager.get.createSSLEngine()
      newPipeline.addLast("ssl", new SslHandler(sslEngine))
    }

    newPipeline.addLast("decoder", new HttpRequestDecoder(
      httpConfig.maxInitialLineLength,
      httpConfig.maxHeaderSizeInBytes,
      httpConfig.maxChunkSizeInBytes))

    if (httpConfig.aggreateChunks) {
      newPipeline.addLast("chunkAggregator", new HttpChunkAggregator(httpConfig.maxLengthInBytes))
    }

    newPipeline.addLast("encoder", new HttpResponseEncoder())
    newPipeline.addLast("chunkWriter", new ChunkedWriteHandler())

    newPipeline.addLast("handler", new RequestHandler(server))

    return newPipeline
  }

  private def getSpdyPipeline(): ChannelPipeline = {
    val newPipeline = Channels.pipeline()

    val sslEngine = server.sslManager.get.createSSLEngine()
    newPipeline.addLast("ssl", new SslHandler(sslEngine))

    NextProtoNego.put(sslEngine, new SpdyServerProvider())
    NextProtoNego.debug = log.isDebugEnabled

    newPipeline.addLast("pipeLineSelector", new ProtocolNegoitationHandler(server))

    return newPipeline
  }
}