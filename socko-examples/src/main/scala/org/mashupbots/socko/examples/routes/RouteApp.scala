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

import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.routes._
import org.mashupbots.socko.utils.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props

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
  // See `TimeProcessor`
  //
  val actorSystem = ActorSystem("RouteExampleActorSystem")

  //
  // STEP #2 - Define Routes
  // Each route dispatches the request to a newly instanced `TimeProcessor` actor for processing.
  // `TimeProcessor` will `stop()` itself after processing each request.
  //
  val routes = Routes({
    
    case HttpRequest(httpRequest) => httpRequest match {
      // *** HOW TO EXTRACT QUERYSTRING VARIABLES AND USE CONCATENATION ***
      // If the timezone is specified on the query string, (like "/time?tz=sydney"), pass the
      // timezone to the TimeProcessor    
      case (GET(Path("/time")) & TimezoneQueryString(timezone)) => {
        actorSystem.actorOf(Props[TimeProcessor]) ! TimeRequest(httpRequest, Some(timezone))
      }

      // *** HOW TO MATCH AND EXTRACT A PATH SEGMENT ***
      // If the timezone is specified on the path (like "/time/sydney"), pass the
      // timezone to the TimeProcessor
      case GET(PathSegments("time" :: timezone :: Nil)) => {
        actorSystem.actorOf(Props[TimeProcessor]) ! TimeRequest(httpRequest, Some(timezone))
      }

      // *** HOW TO MATCH AN EXACT PATH ***
      // No timezone specified, make TimeProcessor return the time in the default timezone
      case GET(Path("/time")) => {
        actorSystem.actorOf(Props[TimeProcessor]) ! TimeRequest(httpRequest, None)
      }

      // If favicon.ico, just return a 404 because we don't have that file
      case Path("/favicon.ico") => {
        httpRequest.writeErrorResponse(HttpResponseStatus.NOT_FOUND, false, "")
      }
    }
    
  })

  object TimezoneQueryString extends QueryStringMatcher("tz")

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes)
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