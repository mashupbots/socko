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

import org.mashupbots.socko.handlers.GET
import org.mashupbots.socko.handlers.Routes
import org.mashupbots.socko.processors.SnoopProcessor
import org.mashupbots.socko.utils.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.ActorSystem
import akka.actor.Props

/**
 * This example shows how to setup a simple route and snoop actor.
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `http://localhost:9991/`.
 *
 * Socko uses Netty to handle incoming requests and AKKA to process them
 *  - Incoming requests are initial executed using threads from the Netty thread pool
 *  - As part of handling a request, `routes` will be called to dispatch it for processing
 *  - Inside our route definition, we instance a new `SnoopProcessor` actor and pass the context to it
 *  - The `SnoopProcessor` actor is executed in AKKA's default thread pool
 */
object SnoopExample extends Logger {

  private var webServer: WebServer = null

  //
  // Step #1
  // Start AKKA system
  //
  val actorSystem = ActorSystem("SnoopExampleActorSystem")

  //
  // Step #2
  // Define routes. Each route dispatches the request to a newly instanced `SnoopProcessor` actor for processing.
  // `SnoopProcessor` will `stop()` itself after processing each request.
  //
  val routes = Routes({
    case ctx @ _ => {
      actorSystem.actorOf(Props[SnoopProcessor]) ! ctx
    }
  })

  //
  // Step #3
  // Instance WebServer and start it. Stop WebServer upon shutdown
  //
  def main(args: Array[String]) {    
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    
    webServer = new WebServer(WebServerConfig(port = 9991), routes)
    webServer.start()
  }

}