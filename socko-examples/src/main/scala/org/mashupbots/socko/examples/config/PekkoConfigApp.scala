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
package org.mashupbots.socko.examples.config

import org.mashupbots.socko.examples.quickstart.HelloHandler
import org.mashupbots.socko.routes._
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.ExtendedActorSystem
import org.apache.pekko.actor.ExtensionId
import org.apache.pekko.actor.ExtensionIdProvider
import org.apache.pekko.actor.Props

/**
 * This example shows how load your web server configuration from Pekko's `application.conf`.
 * It is found in the root class path.  The source file is found in `src/main/resources`.
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `http://localhost:9000/`
 */
object PekkoConfigApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Pekko
  // See `HelloHandler`
  //
  val actorSystem = ActorSystem("PekkoConfigActorSystem")

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

    log.info("Loading configuration from application.conf")
    val myWebServerConfig = MyWebServerConfig(actorSystem)
    log.info("Config is: " + myWebServerConfig.toString)

    val webServer = new WebServer(myWebServerConfig, routes, actorSystem)
    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    System.out.println("Open your browser and navigate to http://localhost:9000")
  }
}

/**
 * Store of our web server configuration.
 *
 * Note that `WebServerConfig` is instanced with the setting named `pekko-config-example`. This name must correspond
 * with `application.conf`.  
 * 
 * For example:
 * {{{
 * pekko-config-example {
 *   server-name=PekkoConfigExample
 *   hostname=localhost
 *   port=9000
 * }
 * }}} 
 */
object MyWebServerConfig extends ExtensionId[WebServerConfig] with ExtensionIdProvider {
  override def lookup = MyWebServerConfig
  override def createExtension(system: ExtendedActorSystem) =
    new WebServerConfig(system.settings.config, "pekko-config-example")
}
