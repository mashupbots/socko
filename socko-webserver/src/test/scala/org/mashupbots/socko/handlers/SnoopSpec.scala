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
package org.mashupbots.socko.handlers

import java.net.HttpURLConnection
import java.net.URL
import java.util.Hashtable

import org.jboss.netty.util.CharsetUtil
import org.junit.runner.RunWith
import org.mashupbots.socko.infrastructure.WebLogFormat
import org.mashupbots.socko.routes.HttpRequest
import org.mashupbots.socko.routes.Path
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.routes.WebSocketFrame
import org.mashupbots.socko.routes.WebSocketHandshake
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec

import com.typesafe.config.ConfigFactory

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props

@RunWith(classOf[JUnitRunner])
class SnoopSpec extends WordSpec with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen with TestHttpClient {

  val akkaConfig =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "DEBUG"
	  }    
    """
  val actorSystem = ActorSystem("SnoopActorSystem", ConfigFactory.parseString(akkaConfig))
  var webServer: WebServer = null
  val port = 9000
  val path = "http://localhost:" + port + "/"

  val routes = Routes({
    case HttpRequest(httpRequest) => httpRequest match {
      case Path("/snoop/") => {
        val name = "SnoopHandler_%s_%s".format(httpRequest.channel.getId, System.currentTimeMillis)
        actorSystem.actorOf(Props[SnoopHandler], name) ! httpRequest
      }
    }
    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/snoop/websocket/") => {
        // For WebSocket processing, we first have to authorize the handshake by setting the "isAllowed" property.
        // This is a security measure to make sure that web sockets can only be established at your specified end points.
        wsHandshake.authorize()
      }
    }
    case WebSocketFrame(wsFrame) => {
      val name = "SnoopHandler_%s_%s".format(wsFrame.channel.getId, System.currentTimeMillis)
      actorSystem.actorOf(Props[SnoopHandler], name) ! wsFrame
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

  "Socko Web Server" should {

    "support HTTP GET" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: GET")
      resp.content should include("REQUEST_URI: /snoop/")

      resp.headers("Date").length should be > 0
      resp.headers("Content-Length").length should be > 0
      resp.headers("Content-Type") should equal("text/plain; charset=UTF-8")
    }

    "support HTTP POST" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      sendPostRequest(conn, URLENCODED_CONTENT_TYPE, CharsetUtil.UTF_8, "userid=joe&password=guessme")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: POST")
      resp.content should include("REQUEST_URI: /snoop/")
      resp.content should include("  userid=joe")
      resp.content should include("  password=guessme")

      resp.headers("Date").length should be > 0
      resp.headers("Content-Length").length should be > 0
      resp.headers("Content-Type") should equal("text/plain; charset=UTF-8")
    }

    "support HTTP PUT" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      sendPutRequest(conn, "text/xml", CharsetUtil.UTF_8, "<abc></abc>")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: PUT")
      resp.content should include("REQUEST_URI: /snoop/")
      resp.content should include("<abc></abc>")

      resp.headers("Date").length should be > 0
      resp.headers("Content-Length").length should be > 0
      resp.headers("Content-Type") should equal("text/plain; charset=UTF-8")
    }

    "support HTTP DELETE" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("DELETE")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: DELETE")
      resp.content should include("REQUEST_URI: /snoop/")

      resp.headers("Date").length should be > 0
      resp.headers("Content-Length").length should be > 0
      resp.headers("Content-Type") should equal("text/plain; charset=UTF-8")
    }

    "support HTTP HEAD" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("HEAD")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      // There is no content returned for HEAD
    }

    "support HTTP OPTIONS" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("OPTIONS")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: OPTIONS")
      resp.content should include("REQUEST_URI: /snoop/")
    }

    "support HTTP TRACE" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("TRACE")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: TRACE")
      resp.content should include("REQUEST_URI: /snoop/")
    }

    "support HTTP file upload" in {
      val text = "hello from the text file"
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]

      val params = new Hashtable[String, String]
      params.put("custom_param", "param_value")
      params.put("custom_param2", "param_value2")

      val req = sendPostFileUpload(conn, params,
        "file_upload_field", "original_filename.txt", "text/plain", text.getBytes(CharsetUtil.UTF_8))

      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: POST")
      resp.content should include("REQUEST_URI: /snoop/")
      resp.content should include("  custom_param2=param_value2")
      resp.content should include("  custom_param=param_value")
      resp.content should include("  File Field=file_upload_field")
      resp.content should include("  File Name=original_filename.txt")
      resp.content should include("  File MIME Type=text/plain")
      resp.content should include("  File Content=" + text)
    }

    "support Web Sockets" in {
      val wsc = new TestWebSocketClient(path + "snoop/websocket/")
      wsc.connect()

      wsc.isConnected should be(true)

      wsc.send("test #1", true)
      wsc.send("test #2", true)
      wsc.send("test #3", true)

      wsc.disconnect()

      val receivedText = wsc.getReceivedText
      receivedText should equal("test #1\ntest #2\ntest #3\n")
    }

    "not connect if web socket path not found" in {
      val wsc = new TestWebSocketClient(path + "snoop/notexist/")
      wsc.connect()
      wsc.isConnected should be(false)
      wsc.disconnect()
    }

    "not connect if web socket path not allowed by route" in {
      val wsc = new TestWebSocketClient(path + "snoop/")
      wsc.connect()
      wsc.isConnected should be(false)
      wsc.disconnect()
    }

    "return 404 if route not found" in {
      val url = new URL(path + "notfound")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)

      resp.status should equal("404")
    }

    "support GZIP compression" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestProperty("Accept-Encoding", "gzip")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: GET")
      resp.content should include("REQUEST_URI: /snoop/")
      resp.content should include("HEADER: Accept-Encoding = gzip")
      resp.headers("Date").length should be > 0
      resp.headers("Content-Encoding") should equal("gzip")
    }

    "support Deflate compression" in {
      val url = new URL(path + "snoop/")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestProperty("Accept-Encoding", "deflate")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content.length should be > 0
      resp.content should include("METHOD: GET")
      resp.content should include("HEADER: Accept-Encoding = deflate")
      resp.headers("Date").length should be > 0
      resp.headers("Content-Encoding") should equal("deflate")
    }

  }
}