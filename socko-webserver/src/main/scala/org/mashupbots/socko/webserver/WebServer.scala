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

import java.util.concurrent.Executors

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.mashupbots.socko.context.ProcessingContext
import org.mashupbots.socko.utils.Logger

import javax.net.ssl.SSLEngine

/**
 * Socko Web Server
 *
 * {{{
 *   val webServer = new WebServer(myWebServerConfig, routes)
 *   webServer.start()
 *   ...
 *   
 *   webServer.stop()
 * }}}
 * 
 * @param config Web server configuration
 * @param routes Routes for processing requests
 */
class WebServer(
  val config: WebServerConfig,
  val routes: PartialFunction[ProcessingContext, Unit]) extends Logger {

  require(config != null)
  config.validate()

  /**
   * Collection of channels that are currently being used
   */
  val allChannels = new DefaultChannelGroup(config.serverName)

  /**
   * Channel factory
   */
  private var channelFactory: NioServerSocketChannelFactory = null

  /**
   * SSL Engine
   */
  val sslManager: Option[SslManager] = if (config.sslConfig.isDefined) Some(new SslManager(this)) else None

  /**
   * Starts the server
   */
  def start(): Unit = {
    if (channelFactory != null) {
      log.info("Socko server '{}' already started", Array(config.serverName))
      return
    }

    allChannels.clear()

    channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

    val bootstrap = new ServerBootstrap(channelFactory)
    bootstrap.setPipelineFactory(new PipelineFactory(WebServer.this))

    config.hostname.split(",").foreach(address => {
      address.trim() match {
        case "0.0.0.0" =>
          allChannels.add(bootstrap.bind(new java.net.InetSocketAddress(config.port)))
        case _ =>
          if (!address.isEmpty) {
            allChannels.add(bootstrap.bind(new java.net.InetSocketAddress(address, config.port)))
          }
      }
    })

    log.info("Socko server '{}' started on {}:{}",
      Array[AnyRef](config.serverName, config.hostname, config.port.toString).toArray)
  }

  /**
   * Stops the server
   */
  def stop(): Unit = {
    val future = allChannels.close()
    future.awaitUninterruptibly()

    allChannels.clear()

    channelFactory.releaseExternalResources()
    channelFactory = null

    log.info("Socko server '{}' stopped", config.serverName)
  }

}