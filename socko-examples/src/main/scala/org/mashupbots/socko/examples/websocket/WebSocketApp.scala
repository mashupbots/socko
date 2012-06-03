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

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.routes._
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala

/**
 * This example shows how to use web sockets with Socko.
 *  - Open your browser and navigate to `http://localhost:8888/html`.
 *  - A HTML page will be displayed
 *  - It will make a web socket connection to `ws://localhost:8888/websocket/`
 *
 * This is a port of the Netty project web socket server example.
 */
object WebSocketApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  // See `WebSocketHandler`.
  //
  val actorSystem = ActorSystem("WebSocketExampleActorSystem")

  //
  // STEP #2 - Define Routes
  // Each route dispatches the request to a newly instanced `WebSocketHandler` actor for processing.
  // `WebSocketHandler` will `stop()` itself after processing the request. 
  //
  val routes = Routes({

    case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/html")) => {
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[WebSocketHandler]) ! httpRequest
      }
      case Path("/favicon.ico") => {
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/") => {
        // To start Web Socket processing, we first have to authorize the handshake.
        // This is a security measure to make sure that web sockets can only be established at your specified end points.
        wsHandshake.authorize()
      }
    }

    case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from the client
      actorSystem.actorOf(Props[WebSocketHandler]) ! wsFrame
    }

  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //

  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open your browser and navigate to http://localhost:8888/html")
  }
}