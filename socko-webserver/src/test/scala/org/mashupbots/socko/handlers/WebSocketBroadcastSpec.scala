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
package org.mashupbots.socko.handlers

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.infrastructure.WebLogFormat
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.Props

/**
 * Test broadcast.  Need its own test so that broadcast does not interfere with other tests 
 */
class WebSocketBroadcastSpec extends WordSpec with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen with TestHttpClient {

  val akkaConfig =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "DEBUG"
	  }    
    """
  val actorSystem = ActorSystem("WsBroadcastActorSystem", ConfigFactory.parseString(akkaConfig))
  var webServer: WebServer = null
  val port = 9003
  val path = "http://localhost:" + port + "/"

  val routes = Routes({
    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/broadcast/") => {
        wsHandshake.authorize()
      }
    }
    case WebSocketFrame(wsFrame) => {
      val name = "SnoopHandler_%s_%s".format(wsFrame.context.name, System.currentTimeMillis)
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

    "support push broadcast" in {
      val wsc1 = new TestWebSocketClient(path + "websocket/broadcast/")
      wsc1.connect()
      wsc1.isConnected should be(true)

      val wsc2 = new TestWebSocketClient(path + "websocket/broadcast/")
      wsc2.connect()
      wsc2.isConnected should be(true)

      Thread.sleep(500)
      webServer.webSocketConnections.writeText("test #1")
      webServer.webSocketConnections.writeText("test #2")
      webServer.webSocketConnections.writeText("test #3")
      Thread.sleep(500)

      wsc1.disconnect()
      wsc2.disconnect()
      
      val receivedText1 = wsc1.getReceivedText
      receivedText1 should equal("test #1\ntest #2\ntest #3\n")

      val receivedText2 = wsc2.getReceivedText
      receivedText2 should equal("test #1\ntest #2\ntest #3\n")      
    }
    
  }
}