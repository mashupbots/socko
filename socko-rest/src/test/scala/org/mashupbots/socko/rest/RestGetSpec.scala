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

import java.net.HttpURLConnection
import java.net.URL

import scala.concurrent.duration._

import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.WebLogFormat
import org.mashupbots.socko.routes.HttpRequest
import org.mashupbots.socko.routes.PathSegments
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestKit

object RestGetSpec {
  val cfg =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "DEBUG"
	  }    
    """  
}

class RestGetSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpec
  with MustMatchers with BeforeAndAfterAll with TestHttpClient with Logger  {
   

    
  def this() = this(ActorSystem("HttpSpec", ConfigFactory.parseString(RestGetSpec.cfg)))
    
  var webServer: WebServer = null
  val port = 9020
  val path = "http://localhost:" + port + "/"
    
  val restRegistry = RestRegistry("org.mashupbots.socko.rest.get", RestConfig("1.0", "/api"))
  val restHandler = system.actorOf(Props(new RestHandler(restRegistry)))
    
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

    webServer = new WebServer(config, routes, system)
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

      resp.status must equal("200")
      resp.content.length must be ( 0)
      
      val url2 = new URL(path + "api/void/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be ( 0)

      val url3 = new URL(path + "api/void/cannot_parse")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("500")
      resp3.content.length must be ( 0)
      
      restHandler ! RestHandlerWorkerCountRequest()
      expectMsgPF(5 seconds) {
        case m: Int => {
          m must be(0)
        }
      }      
    }

  }
}