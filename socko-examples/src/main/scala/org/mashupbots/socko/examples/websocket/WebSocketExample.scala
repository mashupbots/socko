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
package org.mashupbots.socko.examples.websocket

import org.mashupbots.socko.handlers.GET
import org.mashupbots.socko.handlers.Routes
import org.mashupbots.socko.processors.SnoopProcessor
import org.mashupbots.socko.utils.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.actor.ActorSystem
import akka.actor.Props
import org.mashupbots.socko.handlers.Path
import org.mashupbots.socko.context.WsHandshakeProcessingContext
import org.mashupbots.socko.context.WsProcessingContext
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.jboss.netty.handler.codec.http.HttpResponseStatus

/**
 * This example shows how to use web sockets with Socko.
 *
 *  - Navigate to `http://localhost:9992/html` in your browser.
 *  - A HTML page will be displayed
 *  - It will make a web socket connection to `ws://localhost:9992/websocket/`
 *  
 * This is a port of the Netty project web socket server example.
 */
object WebSocketExample extends Logger {
  private var webServer: WebServer = null

  val actorSystem = ActorSystem("WebSocketExampleActorSystem")

  val routes = Routes({
    case ctx @ GET(Path("/html")) => {
      // Return HTML page to establish web socket
      actorSystem.actorOf(Props[WebSocketProcessor]) ! ctx
    }
    case ctx @ Path("/websocket/") => ctx match {
      // For WebSocket processing, we first have to authorize the handshake by setting the "isAllowed" property.
      // This is a security measure to make sure that web sockets can only be established at your specified end points.
      case ctx: WsHandshakeProcessingContext => {
        val hctx = ctx.asInstanceOf[WsHandshakeProcessingContext]
        hctx.isAllowed = true
      }
      // Once handshaking has taken place, the client can then send text to us for processing
      case ctx: WsProcessingContext => {
        actorSystem.actorOf(Props[WebSocketProcessor]) ! ctx
      }
    }
    case ctx @ Path("/favicon.ico") => {
      // If favicon.ico, just return a 404
      val httpContext = ctx.asInstanceOf[HttpRequestProcessingContext]
      httpContext.writeErrorResponse(HttpResponseStatus.NOT_FOUND, false, "")
    }
  })

  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    webServer = new WebServer(WebServerConfig(port = 9992), routes)
    webServer.start()
  }
}