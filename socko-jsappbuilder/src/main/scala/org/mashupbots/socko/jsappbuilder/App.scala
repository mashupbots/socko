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
package org.mashupbots.socko.jsappbuilder

import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.Props
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.routes._
import java.io.File
import org.mashupbots.socko.infrastructure.Logger
import com.typesafe.config.ConfigFactory

/**
 * Command line interface for continuous builds and serving files over HTTP.
 *
 * Expects to find configuration options in a file called `jsappbuilder.conf`.
 */
object Main extends Logger {

  /**
   * Routes
   */
  val routes = Routes({
    case GET(request) => {
      //actorSystem.actorOf(Props[HelloHandler]) ! request
    }
  })

  /**
   *
   */
  def main(args: Array[String]) {
    // Parse command line
    var configFileName = "jsappbuilder.conf"
    if (!args.isEmpty) {
      if (args(0).toLowerCase == "--help") {
        // todo show help
      } else {
        configFileName = args(0)
      }
    }

    // Get config file
    val configFile = new File(configFileName);
    if (configFile.exists) {
      log.info("Using configuration from '{}'", configFile.getAbsolutePath)
    }

    // Start system
    val actorSystem = if (configFile.exists) ActorSystem("jsappbuilder") else
      ActorSystem("jsappbuilder", ConfigFactory.parseFile(configFile))
    val myAppConfig = AppConfig(actorSystem)

    val webServer = new WebServer(myAppConfig.webserver, routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    // Done
    Console.println("jsappbuilder");
  }

}

