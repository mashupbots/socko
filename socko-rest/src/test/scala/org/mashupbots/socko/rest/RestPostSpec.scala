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
import java.util.Date

import org.json4s.NoTypeHints
import org.json4s.native.{Serialization => json}
import org.mashupbots.socko.infrastructure.CharsetUtil
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

object RestPostSpec {
  val cfg =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "DEBUG"
	  }    
    """
}

class RestPostSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpec
  with MustMatchers with BeforeAndAfterAll with TestHttpClient with Logger {

  def this() = this(ActorSystem("HttpSpec", ConfigFactory.parseString(RestPutSpec.cfg)))

  var webServer: WebServer = null
  val port = 9023
  val path = "http://localhost:" + port + "/"

  val restRegistry = RestRegistry("org.mashupbots.socko.rest.post",
    RestConfig("1.0", "http://localhost:9023/api", reportRuntimeException = ReportRuntimeException.All))
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

  "RestPostSpec" should {

    "POST void operations" in {
      val url = new URL(path + "api/void/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "{\"name\":\"Boo\",\"age\":5}")
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content.length must be(0)

      val url2 = new URL(path + "api/void/204")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn2, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "{\"name\":\"Boo\",\"age\":5}")
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("204")
      resp2.content.length must be(0)
    }

    "POST object operations" in {
      val url = new URL(path + "api/object/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "{\"name\":\"Boo\",\"age\":5}")
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content must be("{\"name\":\"Boo\",\"age\":5,\"history\":[]}")
      resp.headers.getOrElse("Content-Type", "") must be("application/json; charset=UTF-8")

      // Non successful return code
      val url2 = new URL(path + "api/object/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn2, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "{\"name\":\"Boo\",\"age\":5}")
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be(0)

      // Nested objects
      val evt1 = org.mashupbots.socko.rest.put.Event(new Date(), "born")
      val evt2 = org.mashupbots.socko.rest.put.Event(new Date(), "Died")
      val data = org.mashupbots.socko.rest.put.Fish("Flounder", 1, List(evt1, evt2))
      implicit val formats = json.formats(NoTypeHints)
      val s = json.write(data)
      log.info("Sending JSON: " + s)

      val url3 = new URL(path + "api/object/200")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn3, "application/json; charset=UTF-8", CharsetUtil.UTF_8, s)
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("200")
      resp3.content must be(s)
      resp3.headers.getOrElse("Content-Type", "") must be("application/json; charset=UTF-8")

      // Empty content = bad request because object is required
      val url4 = new URL(path + "api/object/204")
      val conn4 = url4.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn4, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "")
      val resp4 = getResponseContent(conn4)

      resp4.status must equal("400")
    }

    "POST bytes operations" in {
      val url = new URL(path + "api/bytes/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn, "text/plain; charset=UTF-8", CharsetUtil.UTF_8, "YeeHaa")
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content must be("YeeHaa")

      val url2 = new URL(path + "api/bytes/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn2, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "YeeHaa")
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be(0)

      // Empty byte array
      val url3 = new URL(path + "api/bytes/204")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn3, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "")
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("204")
      resp3.content.length must be(0)
    }

    "POST primitive operations" in {
      val url = new URL(path + "api/primitive/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn, "text/plain; charset=UTF-8", CharsetUtil.UTF_8, "1.23")
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content must be("1.23")

      val url2 = new URL(path + "api/primitive/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn2, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "1.23")
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")

      // Empty content = binding error because body is required
      val url3 = new URL(path + "api/primitive/204")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn3, "application/json; charset=UTF-8", CharsetUtil.UTF_8, "")
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("400")
    }


    "POST custom operations" in {
      val url = new URL(path + "api/custom")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn, "text/plain; charset=UTF-8", CharsetUtil.UTF_8, "hello")
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content must be("hello")

    }    
  }
}