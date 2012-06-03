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
package org.mashupbots.socko.examples.weblog

import org.mashupbots.socko.routes._
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.WebLogEvent
import org.mashupbots.socko.infrastructure.WebLogFormat
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.actor.Actor

/**
 * This example shows how to use a custom web log writer
 */
object CustomWebLogApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  //
  // Make sure your application.conf contains:
  //
  // akka {
  //   event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
  //   loglevel = "DEBUG"
  // }  
  //
  val actorSystem = ActorSystem("CustomWebLogActorSystem")
  val customWebLogWriter = actorSystem.actorOf(Props[CustomWebLogWriter])

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case GET(request) => {
      actorSystem.actorOf(Props[HelloHandler]) ! request
    }
  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    val webLogConfig = Some(WebLogConfig(Some(customWebLogWriter.path.toString)))
    val config = WebServerConfig(webLog = webLogConfig)
    val webServer = new WebServer(config, routes, actorSystem)
    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    System.out.println("Open your browser and navigate to 'http://localhost:8888'.\n" +
      "Try different paths like 'http://localhost:8888/mypath' and see them in the web logs.\n" + 
      "Your web logs is being writtern by " + customWebLogWriter.path.toString)
  }
}

/**
 * Custom web log writer that just dumps the web log event to the console
 */
class CustomWebLogWriter() extends Actor {
  def receive = {
    case evt: WebLogEvent => { System.out.println("CUSTOM WEBLOG WRITER: " + evt.toString) }
  }
}