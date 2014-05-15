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

/**
 * This example can be used to run against the Autobahn test suite
 *  - Install <a href="http://autobahn.ws/testsuite/installation.html>"Autobahn</a> 
 *  - Run AutobahnApp which will start an echo web socket server at `ws://127.0.0.1:9001`
 *  - Run `wstest -m fuzzingclient`
 */
object AutobahnApp extends Logger {
  val actorSystem = ActorSystem("AutobahnActorSystem")
  
  // Instance a singleton handler so that frames are echo'ed in sequence.
  // If we instance a new handler for each frame, Akka may not echo the incoming frames sequentially
  // because instancing a new handler is not guaranteed to be sequential.
  // This is important for passing the Autobahn test suite.
  val handler = actorSystem.actorOf(Props[AutobahnHandler])

  val routes = Routes({

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/") => {
        // To start Web Socket processing, we first have to authorize the handshake.
        // For Autobahn tests, we need a big frame size to pass the limits test
        wsHandshake.authorize(maxFrameSize = Integer.MAX_VALUE)
      }
    }

    case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from the client
      //wsFrame.context.writeAndFlush(wsFrame.wsFrame)
      handler ! wsFrame
    }

  })

  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(port = 9001), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    System.out.println("Run your Autobahn test cases against the end point ws://localhost:9001")
  }
}