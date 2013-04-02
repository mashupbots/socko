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

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.IOUtil
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
  with MustMatchers with BeforeAndAfterAll with TestHttpClient with Logger {

  def this() = this(ActorSystem("HttpSpec", ConfigFactory.parseString(RestGetSpec.cfg)))

  var tempDir: File = null

  var webServer: WebServer = null
  val port = 9020
  val path = "http://localhost:" + port + "/"

  val restRegistry = RestRegistry("org.mashupbots.socko.rest.get",
    RestConfig("1.0", "/api", requestTimeoutSeconds = 2, reportRuntimeException = ReportRuntimeException.All))
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

    tempDir = File.createTempFile("Temp_", "")
    tempDir.delete()
    tempDir.mkdir()

    webServer = new WebServer(config, routes, system)
    webServer.start()
  }

  override def afterAll(configMap: Map[String, Any]) {
    webServer.stop()

    if (tempDir != null) {
      IOUtil.deleteDir(tempDir)
      tempDir = null
    }
  }

  "RestGetSpec" should {

    "GET void operations" in {
      val url = new URL(path + "api/void/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content.length must be(0)

      val url2 = new URL(path + "api/void/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be(0)

      // HEAD
      val url3 = new URL(path + "api/void/200")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      conn3.setRequestMethod("HEAD")
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("200")
      resp3.content.length must be(0)

      //restHandler ! RestHandlerWorkerCountRequest()
      //expectMsgPF(5 seconds) {
      //  case m: Int => {
      //    m must be(0)
      //  }
      //}      
    }

    "GET object operations" in {
      val url = new URL(path + "api/object/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content must be("{\"name\":\"Boo\",\"age\":5}")
      resp.headers.getOrElse("Content-Type", "") must be("application/json; charset=UTF-8")

      val url2 = new URL(path + "api/object/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be(0)

      // HEAD
      val url3 = new URL(path + "api/object/200")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      conn3.setRequestMethod("HEAD")
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("200")
      resp3.content.length must be(0)

    }

    "GET bytes operations" in {
      val url = new URL(path + "api/bytes/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content must be("hello everybody")
      resp.headers.getOrElse("Content-Type", "") must be("text/plain; charset=UTF-8")

      val url2 = new URL(path + "api/bytes/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be(0)

      // HEAD
      val url3 = new URL(path + "api/bytes/200")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      conn3.setRequestMethod("HEAD")
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("200")
      resp3.content.length must be(0)
    }

    "GET primitive operations" in {
      val url = new URL(path + "api/primitive/200")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      DateUtil.parseISO8601Date(resp.content)
      resp.headers.getOrElse("Content-Type", "") must be("text/plain; charset=UTF-8")

      val url2 = new URL(path + "api/primitive/404")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be(0)

      // HEAD
      val url3 = new URL(path + "api/primitive/200")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      conn3.setRequestMethod("HEAD")
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("200")
      resp3.content.length must be(0)
    }

    "GET stream URL operations" in {
      val sb = new StringBuilder
      for (i <- 1 to 1000) sb.append("abc")
      val content = sb.toString

      val file = new File(tempDir, "streamurl.txt")
      IOUtil.writeTextFile(file, content, CharsetUtil.UTF_8)

      val url = new URL(path + "api/streamurl/200?sourceURL=" + URLEncoder.encode(file.toURI().toURL().toString(), "UTF-8"))
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status must equal("200")
      resp.content must equal(content)
      resp.headers.getOrElse("Content-Type", "") must be("text/plain; charset=UTF-8")

      val url2 = new URL(path + "api/streamurl/404?sourceURL=notrequired")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("404")
      resp2.content.length must be(0)

      // HEAD
      val url3 = new URL(path + "api/streamurl/200?sourceURL=" + URLEncoder.encode(file.toURI().toURL().toString(), "UTF-8"))
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      conn3.setRequestMethod("HEAD")
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("200")
      resp3.content.length must be(0)
    }

    "Correctly handle binding errors" in {
      // Not route
      val url = new URL(path + "api/no_route")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status must equal("400")
      log.info(s"Error message: ${resp.content}")

      // Required query string "sourceURL" not present
      val url2 = new URL(path + "api/streamurl/200")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)

      resp2.status must equal("400")
      resp2.content.length must not be (0)
      log.info(s"Error message: ${resp2.content}")

      val url3 = new URL(path + "api/void/cannot_parse")
      val conn3 = url3.openConnection().asInstanceOf[HttpURLConnection]
      val resp3 = getResponseContent(conn3)

      resp3.status must equal("400")
      resp3.content.length must not be (0)
    }

    "Correctly handle errors with processing actors" in {
      // Exception in processor actor
      val url = new URL(path + "api/error/exception")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)
      resp.status must equal("500")
      resp.content must equal("Timed out")

      // Processor actor did not respond in time
      val url2 = new URL(path + "api/error/timeout")
      val conn2 = url2.openConnection().asInstanceOf[HttpURLConnection]
      val resp2 = getResponseContent(conn2)
      resp2.status must equal("500")
      resp2.content must equal("Timed out")      
    }

  }
}