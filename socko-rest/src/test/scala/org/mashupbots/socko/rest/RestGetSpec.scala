//
// Copyright 2013 Vibul Imtarnasan, David Bolton and Socko contributors.
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
package org.mashupbots.socko.rest

import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.routes._
import akka.actor.Props
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServerConfig
import org.mashupbots.socko.infrastructure.WebLogFormat
import org.mashupbots.socko.rest.get.GetVoidProcessor
import org.scalatest.BeforeAndAfterAll
import java.net.URL
import java.net.HttpURLConnection

class RestGetSpec extends WordSpec with ShouldMatchers with BeforeAndAfterAll with TestHttpClient with Logger {

  val akkaConfig =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "DEBUG"
	  }    
    """
  val actorSystem = ActorSystem("GetActorSystem", ConfigFactory.parseString(akkaConfig))
  var webServer: WebServer = null
  val port = 9020
  val path = "http://localhost:" + port + "/"
  
  val voidProcessor = actorSystem.actorOf(Props[GetVoidProcessor], "GetVoidProcessor")
  log.info("GetVoidProcessor=" + voidProcessor.path)
  
  val restRegistry = RestRegistry("org.mashupbots.socko.rest.get", RestConfig("1.0", "/api"))
  val restHandler = actorSystem.actorOf(Props(new RestHandler(restRegistry)))
  
  val routes = Routes({
    case HttpRequest(httpRequest) => httpRequest match {
      case PathSegments("api" :: x) => {
        restHandler ! httpRequest
      }
    }
  })

  override def beforeAll(configMap: Map[String, Any]) {
    // Make all content compressible to pass our tests
    val httpConfig = HttpConfig(minCompressibleContentSizeInBytes = 0)
    val webLogConfig = Some(WebLogConfig(None, WebLogFormat.Common))
    val config = WebServerConfig(port = port, webLog = webLogConfig, http = httpConfig)

    webServer = new WebServer(config, routes, actorSystem)
    webServer.start()
  }

  override def afterAll(configMap: Map[String, Any]) {
    webServer.stop()
  }
  
  "RestGetSpec" should {

    "GET void operations" in {
      val url = new URL(path + "api/void/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be ( 0)
      
      val url2 = new URL(path + "api/void/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status should equal("404")
      resp2.content.length should be ( 0)
      
    }

  }
}