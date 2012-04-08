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

import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.Channels
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedWriteHandler

/**
 * Creates a new channel pipeline for each Netty channel (network connection)
 * 
 * @param server The web server instancing the pipeline
 */
class PipelineFactory(server: WebServer) extends ChannelPipelineFactory {

  /**
   * Returns a new channel pipeline instance to handle our channel
   */
  def getPipeline: ChannelPipeline = {
    val newPipeline = Channels.pipeline()

    if (server.sslEngine.isDefined) {
      newPipeline.addLast("ssl", new SslHandler(server.sslEngine.get))
    }

    newPipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192))

    if (server.config.processingConfig.aggreateChunks) {
      newPipeline.addLast("chunkAggregator",
        new HttpChunkAggregator(server.config.processingConfig.maxLengthInMB * 1024 * 1024))
    }

    newPipeline.addLast("encoder", new HttpResponseEncoder())
    newPipeline.addLast("chunkWriter", new ChunkedWriteHandler())

    newPipeline.addLast("handler", new RequestHandler(server.routes, server.allChannels))

    return newPipeline
  }

}