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
package org.mashupbots.socko.examples.rest

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers.StaticContentHandler
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.handlers.StaticResourceRequest
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.rest.ReportRuntimeException
import org.mashupbots.socko.rest.RestConfig
import org.mashupbots.socko.rest.RestHandler
import org.mashupbots.socko.rest.RestRegistry
import org.mashupbots.socko.routes.GET
import org.mashupbots.socko.routes.HttpRequest
import org.mashupbots.socko.routes.Path
import org.mashupbots.socko.routes.PathSegments
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.FromConfig

/**
 * This example shows how use [[org.mashupbots.socko.handler.StaticContentHandler]] to download files and
 * [[org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder]] to process file uploads.
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `http://localhost:8888`.
 */
object RestApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  //
  // We are going to start StaticContentHandler actor within a router.
  // There will be 5 instances, each instance having its own thread since there is a lot of blocking IO.
  //
  // RestHandler will also be started within a router in order to process incoming REST requests
  //
  // As an example of using a router for REST processing, the UserProcessor will also be started within a 
  // router in order to process incoming user api requests
  //
  val actorConfig = """
	my-pinned-dispatcher {
	  type=PinnedDispatcher
	  executor=thread-pool-executor
	}
	akka {
	  event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
	  loglevel=DEBUG
	  actor {
	    deployment {
	      /static-file-router {
	        router = round-robin
	        nr-of-instances = 5
	      }
	      /rest-router {
	        router = round-robin
	        nr-of-instances = 5
	      }
	      /user-api-router {
	        router = round-robin
	        nr-of-instances = 5
	      }
	    }
	  }
	}"""

  val actorSystem = ActorSystem("RestExampleActorSystem", ConfigFactory.parseString(actorConfig))

  val staticContentHandlerRouter = actorSystem.actorOf(Props(new StaticContentHandler(StaticContentHandlerConfig()))
    .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")

  val restRegistry = RestRegistry("org.mashupbots.socko.examples.rest",
    RestConfig("1.0", "http://localhost:8888/api", reportRuntimeException = ReportRuntimeException.All))

  val restRouter = actorSystem.actorOf(Props(new RestHandler(restRegistry)).withRouter(FromConfig()), "rest-router")

  val userProcessorRouter = actorSystem.actorOf(Props[UserProcessor].withRouter(FromConfig()), "user-api-router")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case HttpRequest(request) => request match {
      case PathSegments("swagger-ui" :: relativePath) => {
        // Serve the static swagger-ui content from resources
        staticContentHandlerRouter ! new StaticResourceRequest(request, relativePath.mkString("swaggerui/", "/", ""))
      }
      case PathSegments("api" :: relativePath) => {
        // REST API - just pass the request to the handler for processing
        restRouter ! request
      }
      case GET(Path("/favicon.ico")) => {
        request.response.write(HttpResponseStatus.NOT_FOUND)
      }      
    }
  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    // Start web server
    val config = WebServerConfig(webLog = Some(WebLogConfig()))
    val webServer = new WebServer(config, routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run {
        webServer.stop()
      }
    })
    webServer.start()

    System.out.println("Open your browser and navigate to http://localhost:8888/swagger-ui/index.html")
  }
}
