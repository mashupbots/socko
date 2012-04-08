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
 * This example shows how to setup a simple route and create a simple processor actor.
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `http://localhost:9999/time/` to get the local time.
 *  - You can also get a specific timezone by navigate to `http://localhost:9999/time/{tz}`.
 *    or to `http://localhost:9999/time?tz={tz}` where tz is the timezone id  or city name.
 *
 * Socko uses Netty to handle incoming requests and AKKA to process them
 *  - Incoming requests are initial executed using threads from the Netty thread pool
 *  - As part of handling a request, `routes` will be called to dispatch it for processing
 *  - Inside our route definition, we instance a new `SnoopProcessor` actor and pass the context to it
 *  - The `SnoopProcessor` actor is executed in AKKA's default thread pool
 */
object QuickStartApp extends Logger {

  private var webServer: WebServer = null

  //
  // Step #1
  // Start AKKA system
  //
  val actorSystem = ActorSystem("QuickStartExampleActorSystem")

  //
  // Step #2
  // Define routes. Each route dispatches the request to a newly instanced `TimeProcessor` actor for processing.
  // `TimeProcessor` will `stop()` itself after processing each request.
  //
  val routes = Routes({
    case ctx @ GET(Path("/time")) & TimezoneQueryStringRegex(m) => {
      // If the timezone is specified on the query string, (like "/time?tz=sydney"), pass the
      // timezone to the TimeProcessor
      val timezone = m.group(1)
      val request = TimeRequest(ctx.asInstanceOf[HttpRequestProcessingContext], Some(timezone))
      actorSystem.actorOf(Props[TimeProcessor]) ! request
    }
    case ctx @ GET(Path(PathSegments("time" :: timezone :: Nil))) => {
      // If the timezone is specified on the path (like "/time/pst"), pass the
      // timezone to the TimeProcessor
      val request = TimeRequest(ctx.asInstanceOf[HttpRequestProcessingContext], Some(timezone))
      actorSystem.actorOf(Props[TimeProcessor]) ! request
    }
    case ctx @ GET(Path("/time")) => {
      // No timezone specified, get the TimeProcessor return the time in the default timezone
      val request = TimeRequest(ctx.asInstanceOf[HttpRequestProcessingContext], None)
      actorSystem.actorOf(Props[TimeProcessor]) ! request
    }
    case ctx @ Path("/favicon.ico") => {
      // If favicon.ico, just return a 404 because we don't have that file
      val httpContext = ctx.asInstanceOf[HttpRequestProcessingContext]
      httpContext.writeErrorResponse(HttpResponseStatus.NOT_FOUND, false, "")
    }    
  })
  
  object TimezoneQueryStringRegex extends QueryStringRegex("""tz=([a-zA-Z0-9/]+)""".r)

  //
  // Step #3
  // Instance WebServer and start it. Stop WebServer upon shutdown
  //
  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    webServer = new WebServer(WebServerConfig(port = 9999), routes)
    webServer.start()
  }

}