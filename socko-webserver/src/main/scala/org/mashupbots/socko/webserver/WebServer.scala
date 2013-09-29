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

import scala.collection.JavaConversions._

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelOption
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.GlobalEventExecutor

import java.util.concurrent.CountDownLatch
import org.mashupbots.socko.events.SockoEvent
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.WebLogWriter

import akka.actor.{ActorRefFactory, ActorRef, Props}
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

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
 * @param actorFactory Actor factory (such as an ActorSystem) that can be used to create Socko actors
 */
class WebServer(
  val config: WebServerConfig,
  val routes: PartialFunction[SockoEvent, Unit],
  val actorFactory: ActorRefFactory) extends Logger {

  require(config != null)
  config.validate()

  /**
   * Collection of channels that are currently being used
   */
  val allChannels = new DefaultChannelGroup(config.serverName, GlobalEventExecutor.INSTANCE)

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
    Some(actorFactory.actorOf(Props(new WebLogWriter(config.webLog.get.format))))
  } else {
    // Use custom provided web log writer
    // TODO - change to non blocking
    implicit val timeout = Timeout(5 seconds)
    val actorRef = Await.result(actorFactory.actorSelection(config.webLog.get.customActorPath.get).resolveOne(), timeout.duration)
    Some(actorRef)
  }

  /**
   * Starts the server
   */
  def start(): Unit = {
    
    allChannels.clear()

    val bossGroup = new NioEventLoopGroup
    val workerGroup = new NioEventLoopGroup
    val bootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, config.tcp.noDelay.getOrElse(true).asInstanceOf[java.lang.Boolean])
    if (config.tcp.sendBufferSize.isDefined) {
      bootstrap.childOption[java.lang.Integer](ChannelOption.SO_SNDBUF, config.tcp.sendBufferSize.get)
    }
    if (config.tcp.receiveBufferSize.isDefined) {
      // Thanks to VertX. We need to set a FixedReceiveBufferSizePredictor, since otherwise Netty will ignore our 
      // setting and use an adaptive buffer which can get very large
      bootstrap.childOption[Integer](ChannelOption.SO_RCVBUF, config.tcp.receiveBufferSize.get)
      // ReceiveBufferSizePredictor is not found in netty 4 
      // bootstrap.setOption("child.receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(1024))
    }
    if (config.tcp.keepAlive.isDefined) {
      bootstrap.childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, config.tcp.keepAlive.get)
    }    
    if (config.tcp.soLinger.isDefined) {
      bootstrap.childOption[java.lang.Integer](ChannelOption.SO_LINGER, config.tcp.soLinger.get)
    }
    if (config.tcp.trafficClass.isDefined) {
      bootstrap.childOption[java.lang.Integer](ChannelOption.IP_TOS, config.tcp.trafficClass.get)
    }
    if (config.tcp.reuseAddress.isDefined) {
      bootstrap.childOption[java.lang.Boolean](ChannelOption.SO_REUSEADDR, config.tcp.reuseAddress.get)
    }
    if (config.tcp.acceptBackLog.isDefined) {
      bootstrap.childOption[java.lang.Integer](ChannelOption.SO_BACKLOG, config.tcp.acceptBackLog.get)
    }
    
    bootstrap.childHandler(new PipelineFactory(this))

    val bindFutures = config.hostname.split(",").collect(address => {
        address.trim match {
          case "0.0.0.0" =>
            bootstrap.bind(config.port)
          case _ if (!address.isEmpty) =>
            bootstrap.bind(address, config.port)
        }
      })

    allChannels.addAll(bindFutures.map(_.channel).toList)

    // Wait until the bound ports are ready to accept connections.
    // This is required to avoid connection refused exceptions during testing.
    val latch = new CountDownLatch(bindFutures.length)
    val bindFutureListener = new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) = {
        latch.countDown
      }
    }
    bindFutures.foreach(_.addListener(bindFutureListener))
    latch.await
    
    log.info("Socko server '{}' started on {}:{}", config.serverName, config.hostname, config.port.toString)
  }

  /**
   * Stops the server
   */
  def stop(): Unit = {
    val future = allChannels.close()
    future.awaitUninterruptibly()

    allChannels.clear()

    log.info("Socko server '{}' stopped", config.serverName)
  }

}