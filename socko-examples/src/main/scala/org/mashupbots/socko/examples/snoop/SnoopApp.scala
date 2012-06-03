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
package org.mashupbots.socko.examples.snoop

import org.mashupbots.socko.handlers.SnoopHandler
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props

/**
 * This example shows how to setup a simple route and snoop actor.
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `http://localhost:8888/`.
 */
object SnoopApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  // `SnoopHandler` actor already defined.
  //
  val actorSystem = ActorSystem("SnoopExampleActorSystem")

  //
  // STEP #2 - Define Routes
  // Dispatch the event to a newly instanced `SnoopHandler` actor for processing.
  // `SnoopHandler` will `stop()` itself after processing each request.
  //
  val routes = Routes({
    case request => {
      actorSystem.actorOf(Props[SnoopHandler]) ! request
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

    System.out.println("Open your browser and navigate to http://localhost:8888")
  }

}