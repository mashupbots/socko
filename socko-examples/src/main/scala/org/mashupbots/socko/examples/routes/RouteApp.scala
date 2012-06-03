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
package org.mashupbots.socko.examples.routes

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.routes._
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala

/**
 * This example shows how use route extractors.
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `http://localhost:8888/time/` to get the local time.
 *  - You can also get a specific timezone by specifying a city name
 *    - `http://localhost:8888/time/sydney`, or
 *    - `http://localhost:8888/time?tz=london`
 *
 * Refer to our unit test cases for more examples of routes and different route extractors.
 */
object RouteApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  // See `TimeHandler`
  //
  val actorSystem = ActorSystem("RouteExampleActorSystem")

  //
  // STEP #2 - Define Routes
  // Each route dispatches the request to a newly instanced `TimeHandler` actor for processing.
  // `TimeHandler` will `stop()` itself after processing each request.
  //
  val routes = Routes({
    
    case HttpRequest(request) => request match {
      // *** HOW TO EXTRACT QUERYSTRING VARIABLES AND USE CONCATENATION ***
      // If the timezone is specified on the query string, (like "/time?tz=sydney"), pass the
      // timezone to the TimeHandler    
      case (GET(Path("/time")) & TimezoneQueryString(timezone)) => {
        actorSystem.actorOf(Props[TimeHandler]) ! TimeRequest(request, Some(timezone))
      }

      // *** HOW TO MATCH AND EXTRACT A PATH SEGMENT ***
      // If the timezone is specified on the path (like "/time/sydney"), pass the
      // timezone to the TimeHandler
      case GET(PathSegments("time" :: timezone :: Nil)) => {
        actorSystem.actorOf(Props[TimeHandler]) ! TimeRequest(request, Some(timezone))
      }

      // *** HOW TO MATCH AN EXACT PATH ***
      // No timezone specified, make TimeHandler return the time in the default timezone
      case GET(Path("/time")) => {
        actorSystem.actorOf(Props[TimeHandler]) ! TimeRequest(request, None)
      }

      // If favicon.ico, just return a 404 because we don't have that file
      case Path("/favicon.ico") => {
        request.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }
    
  })

  object TimezoneQueryString extends QueryStringField("tz")

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open your browser and navigate to: ")
    System.out.println("  http://localhost:8888/time")
    System.out.println("  http://localhost:8888/time/sydney")
    System.out.println("  http://localhost:8888/time?tz=london")
  }

}