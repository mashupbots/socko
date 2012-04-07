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

import org.mashupbots.socko.webserver.WebServer
import akka.actor.ActorRef
import java.io.File
import akka.actor.ActorSystem
import org.mashupbots.socko.handlers.Routes
import org.mashupbots.socko.handlers.GET
import org.mashupbots.socko.handlers.Path
import org.mashupbots.socko.handlers.PathSegments
import org.mashupbots.socko.processors.StaticFileRequest
import org.mashupbots.socko.context.HttpRequestProcessingContext
import akka.actor.Props
import org.mashupbots.socko.processors.StaticFileProcessor
import org.mashupbots.socko.webserver.WebServerConfig
import akka.routing.FromConfig
import org.mashupbots.socko.utils.Logger

object SnoopExample extends Logger {

  private var webServer: WebServer = null
  val port = 8889
  val path = "http://localhost:" + port + "/"
  var router: ActorRef = null
  var rootDir: File = new File("/home/vibul/tmp/vvv")
  var tempDir: File = new File("/home/vibul/tmp/tmp")
  var browserCacheSeconds = 60 * 60 * 24

  val actorSystem = ActorSystem("SnoopExampleActorSystem")

  val routes = Routes({
    case ctx @ GET(Path(PathSegments("files" :: relativePath))) => {
      val request = new StaticFileRequest(
        ctx.asInstanceOf[HttpRequestProcessingContext],
        rootDir,
        new File(rootDir, relativePath.mkString("/", "/", "")),
        tempDir,
        60,
        60)
      router ! request
    }
  })

  def main(args: Array[String]) {
    // Start routers
    router = actorSystem.actorOf(Props[StaticFileProcessor]
      .withRouter(FromConfig()).withDispatcher("id"), "a")

    log.debug("hello")
    
    webServer = new WebServer(WebServerConfig(port = 9999), routes)
    webServer.start()
    
  }
   
}