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
import org.mashupbots.socko.events.SockoEvent
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.WebLogWriter
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor

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
 * @param actorSystem Actor system that can be used to host Socko actors
 */
class WebServer(
  val config: WebServerConfig,
  val routes: PartialFunction[SockoEvent, Unit],
  val actorSystem: ActorSystem) extends Logger {

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
  val sslManager: Option[SslManager] = if (config.ssl.isEmpty) None else
    Some(new SslManager(this))

  /**
   * Actor to which web log events will be sent
   */
  val webLogWriter: Option[ActorRef] = if (config.webLog.isEmpty) {
    // Web log turned off
    None
  } else if (config.webLog.get.customActorPath.isEmpty) {
    // Turn on default web log writer
    Some(actorSystem.actorOf(Props(new WebLogWriter(config.webLog.get.format))))
  } else {
    // Use custom provided web log writer
    Some(actorSystem.actorFor(config.webLog.get.customActorPath.get))
  }

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

    bootstrap.setOption("child.tcpNoDelay", config.tcpConfig.noDelay.getOrElse(true))
    if (config.tcpConfig.sendBufferSize.isDefined) {
      bootstrap.setOption("child.sendBufferSize", config.tcpConfig.sendBufferSize.get)
    }
    if (config.tcpConfig.receiveBufferSize.isDefined) {
      // Thanks to VertX. We need to set a FixedReceiveBufferSizePredictor, since otherwise Netty will ignore our setting and use an 
      // adaptive buffer which can get very large
      bootstrap.setOption("child.receiveBufferSize", config.tcpConfig.receiveBufferSize.get)
      bootstrap.setOption("child.receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(1024))
    }
    if (config.tcpConfig.keepAlive.isDefined) {
      bootstrap.setOption("child.keepAlive", config.tcpConfig.keepAlive.get)
    }    
    if (config.tcpConfig.soLinger.isDefined) {
      bootstrap.setOption("child.soLinger", config.tcpConfig.soLinger.get)
    }
    if (config.tcpConfig.trafficClass.isDefined) {
      bootstrap.setOption("child.trafficClass", config.tcpConfig.trafficClass.get);
    }
    if (config.tcpConfig.reuseAddress.isDefined) {
      bootstrap.setOption("child.reuseAddress", config.tcpConfig.reuseAddress.get);
    }
    if (config.tcpConfig.trafficClass.isDefined) {
      bootstrap.setOption("child.backlog", config.tcpConfig.trafficClass.get);
    }

    bootstrap.setPipelineFactory(new PipelineFactory(this))

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