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
package org.mashupbots.socko.examples.quickstart

import org.mashupbots.socko.routes._
import org.mashupbots.socko.utils.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props

/**
 * This example shows how to setup a simple route and create a simple processor actor.
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `http://localhost:8888/`
 *
 * Socko uses Netty to handle incoming requests and Akka to process them
 *  - Incoming requests are initial executed using threads from the Netty thread pool
 *  - As part of handling a request, `routes` will be called to dispatch it for processing
 *  - Inside our route definition, we instance a new `HelloProcessor` actor and pass the context to it
 *  - The `HelloProcessor` actor is executed in Akka default thread pool
 */
object HelloApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  // See `HelloProcessor`
  //
  val actorSystem = ActorSystem("HelloExampleActorSystem")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case ctx @ GET(_) => {
      actorSystem.actorOf(Props[HelloProcessor]) ! ctx
    }
  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes)
    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    System.out.println("Open your browser and navigate to http://localhost:8888")
  }

}