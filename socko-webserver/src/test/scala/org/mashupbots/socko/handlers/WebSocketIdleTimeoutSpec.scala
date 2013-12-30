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

import scala.concurrent.duration._

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
class WebSocketIdleTimeoutSpec extends WordSpec with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen with TestHttpClient {

  val akkaConfig =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "DEBUG"
	  }    
    """
  val actorSystem = ActorSystem("IdleTimeoutActorSystem", ConfigFactory.parseString(akkaConfig))
  var webServer: WebServer = null
  val port = 9004
  val path = "http://localhost:" + port + "/"

  val routes = Routes({
    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/1/") => {
        wsHandshake.authorize(onComplete = Some(testWebSocket1OnComplete), onClose = Some(testWebSocket1OnClose))
      }
      case Path("/websocket/2/") => {
        wsHandshake.authorize(onComplete = Some(testWebSocket2OnComplete), onClose = Some(testWebSocket2OnClose))
      }
    }
    case WebSocketFrame(wsFrame) => {
      val name = "SnoopHandler_%s_%s".format(wsFrame.context.name, System.currentTimeMillis)
      actorSystem.actorOf(Props[SnoopHandler], name) ! wsFrame
    }
  })

  var testWebSocket1Id = ""
  var testWebSocket1Closed = false
  def testWebSocket1OnComplete(webSocketId: String) {
    testWebSocket1Id = webSocketId
  }
  def testWebSocket1OnClose(webSocketId: String) {
    testWebSocket1Closed = (testWebSocket1Id == webSocketId)
  }
  
  var testWebSocket2Id = ""
  var testWebSocket2Closed = false
  def testWebSocket2OnComplete(webSocketId: String) {
    testWebSocket2Id = webSocketId
  }
  def testWebSocket2OnClose(webSocketId: String) {
    testWebSocket2Closed = (testWebSocket2Id == webSocketId)
  }
  
  override def beforeAll(configMap: Map[String, Any]) {
    // Make all content compressible to pass our tests
    val httpConfig = HttpConfig(minCompressibleContentSizeInBytes = 0)
    val webLogConfig = Some(WebLogConfig(None, WebLogFormat.Common))
    
    // Set idle timeout to 1 second
    val config = WebServerConfig(port = port, idleConnectionTimeout = 1 second, webLog = webLogConfig, http = httpConfig)

    webServer = new WebServer(config, routes, actorSystem)
    webServer.start()
  }

  override def afterAll(configMap: Map[String, Any]) {
    webServer.stop()
  }

  "Socko Web Server" should {

    "support idle timeout" in {
      val wsc1 = new TestWebSocketClient(path + "websocket/1/")
      wsc1.connect()
      wsc1.isConnected should be(true)

      val wsc2 = new TestWebSocketClient(path + "websocket/2/")
      wsc2.connect()
      wsc2.isConnected should be(true)

      // Write to socket #1 so it does not timeout (set at 1 second)
      Thread.sleep(300)
      webServer.webSocketConnections.writeText("test #1", testWebSocket1Id)
      Thread.sleep(300)
      webServer.webSocketConnections.writeText("test #2", testWebSocket1Id)
      Thread.sleep(300)
      webServer.webSocketConnections.writeText("test #3", testWebSocket1Id)
      Thread.sleep(300)
      webServer.webSocketConnections.writeText("test #4", testWebSocket1Id)
      Thread.sleep(300)
      webServer.webSocketConnections.writeText("test #5", testWebSocket1Id)
      Thread.sleep(300)

      // Check that #1 is connection and #2 should have timed out and closed
      webServer.webSocketConnections.isConnected(testWebSocket1Id) should be (true)
      webServer.webSocketConnections.isConnected(testWebSocket2Id) should be (false)
      
      testWebSocket1Closed should be (false)
      testWebSocket2Closed should be (true)

      wsc1.isConnected should be(true)
      wsc2.isConnected should be(false)            

      // Finish
      wsc1.disconnect()
    }
    
  }
}