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
 * Basic web socket tests performed in SnoopSpec.
 *
 * These test are for more advanced web socket features
 */
class WebSocketSpec extends WordSpec with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen with TestHttpClient {

  val akkaConfig =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "DEBUG"
	  }    
    """
  val actorSystem = ActorSystem("SnoopActorSystem", ConfigFactory.parseString(akkaConfig))
  var webServer: WebServer = null
  val port = 9002
  val path = "http://localhost:" + port + "/"

  val routes = Routes({
    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/subprotocols/") => {
        wsHandshake.authorize(subprotocols = "chat")
      }
      case Path("/websocket/callbacks/") => {
        wsHandshake.authorize(onComplete = Some(testOnCompleteCallback), onClose = Some(testOnCloseCallback))
      }
      case Path("/websocket/identifier/") => {
        wsHandshake.authorize(onComplete = Some(testOnCompleteIdentifier), onClose = Some(testOnCloseIdentifier))
      }
      case Path("/websocket/maxframesize/") => {
        wsHandshake.authorize(maxFrameSize = 10)
      }
      case Path("/websocket/standard/") => {
        wsHandshake.authorize()
      }
    }
    case WebSocketFrame(wsFrame) => {
      val name = "SnoopHandler_%s_%s".format(wsFrame.context.name, System.currentTimeMillis)
      actorSystem.actorOf(Props[SnoopHandler], name) ! wsFrame
    }
  })

  //
  // Used in Test Callback
  //
  var testCallbackWebSocketId = ""
  var testCallbackWebSocketClosed = false
  def testOnCompleteCallback(webSocketId: String) {
    // System.out.println(s"Web Socket $webSocketId connected")
    testCallbackWebSocketId = webSocketId
    webServer.webSocketConnections.writeText("Hello - we have completed the handshake", webSocketId)
  }
  def testOnCloseCallback(webSocketId: String) {
    // System.out.println(s"Web Socket $webSocketId closed")
    testCallbackWebSocketClosed = (testCallbackWebSocketId == webSocketId)
  }

 
  //
  // Used in Test Identifier
  //
  var testIdentifierWebSocketId = ""
  var testIdentifierWebSocketClosed = false
  def testOnCompleteIdentifier(webSocketId: String) {
    // System.out.println(s"Web Socket $webSocketId connected")
    testIdentifierWebSocketId = webSocketId
  }
  def testOnCloseIdentifier(webSocketId: String) {
    // System.out.println(s"Web Socket $webSocketId closed")
    testIdentifierWebSocketClosed = (testIdentifierWebSocketId == webSocketId)
  }

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

    "support Web Sockets subprotocols" in {
      val wsc = new TestWebSocketClient(path + "websocket/subprotocols/", "chat")
      wsc.connect()

      wsc.isConnected should be(true)

      wsc.send("test #1", true)
      wsc.send("test #2", true)
      wsc.send("test #3", true)

      wsc.disconnect()

      val receivedText = wsc.getReceivedText
      receivedText should equal("test #1\ntest #2\ntest #3\n")
    }

    "support a range of Web Sockets subprotocols" in {
      val wsc = new TestWebSocketClient(path + "websocket/subprotocols/", "p1,p2,p3,chat,p4")
      wsc.connect()

      wsc.isConnected should be(true)

      wsc.send("test #1", true)
      wsc.send("test #2", true)
      wsc.send("test #3", true)

      wsc.disconnect()

      val receivedText = wsc.getReceivedText
      receivedText should equal("test #1\ntest #2\ntest #3\n")
    }

    "not support unrecognised Web Sockets subprotocols" in {
      val wsc = new TestWebSocketClient(path + "websocket/subprotocols/", "dontknow")
      wsc.connect()
      wsc.isConnected should be(false)
      wsc.disconnect()
    }

    "not support frames too big" in {
      val wsc = new TestWebSocketClient(path + "websocket/maxframesize/")
      wsc.connect()
      wsc.isConnected should be(true)
      
      wsc.send("0123456789", true)      

      // Max frame size should throw an exception on the server and cause the channel to close
      wsc.send("01234567890123456789", false)
      Thread.sleep(1000)
      wsc.isConnected should be(false)

      wsc.disconnect()
    }

    "support callback after handshake and on close" in {
      val wsc = new TestWebSocketClient(path + "websocket/callbacks/")
      wsc.connect()
      wsc.isConnected should be(true)

      // Wait for message from callback to arrive 
      Thread.sleep(500)
      wsc.channelData.textBuffer.length should be > (0)
      wsc.channelData.textBuffer.toString.startsWith("Hello") should be(true)      

      // Check that the web socket id is present and socket not closed
      testCallbackWebSocketId.length should be > 0
      webServer.webSocketConnections.isConnected(testCallbackWebSocketId) should be (true)
      
      // Disconnect and check that the server registered that the connection is closed
      wsc.disconnect()
      Thread.sleep(500)
      webServer.webSocketConnections.isConnected(testCallbackWebSocketId) should be (false)
    }

    "not connect if web socket path not found" in {
      val wsc = new TestWebSocketClient(path + "snoop/notexist/")
      wsc.connect()
      wsc.isConnected should be(false)
      wsc.disconnect()
    }
    
    "support push to specific web socket" in {
      val wsc1 = new TestWebSocketClient(path + "websocket/identifier/")
      wsc1.connect()
      wsc1.isConnected should be(true)

      val wsc2 = new TestWebSocketClient(path + "websocket/standard/")
      wsc2.connect()
      wsc2.isConnected should be(true)

      // Messages sent to 1 socket should not be received by another
      webServer.webSocketConnections.writeText("test #1", testIdentifierWebSocketId)
      webServer.webSocketConnections.writeText("test #2", testIdentifierWebSocketId)
      webServer.webSocketConnections.writeText("test #3", testIdentifierWebSocketId)
      Thread.sleep(500)
      
      val receivedText1 = wsc1.getReceivedText
      receivedText1 should equal("test #1\ntest #2\ntest #3\n")

      val receivedText2 = wsc2.getReceivedText
      receivedText2 should equal("")
      
      // Close 1 socket should not close another
      webServer.webSocketConnections.close(testIdentifierWebSocketId)
      Thread.sleep(500)
      webServer.webSocketConnections.isConnected(testCallbackWebSocketId) should be (false)
      testIdentifierWebSocketClosed should be (true)

      wsc1.isConnected should be(false)
      wsc2.isConnected should be(true)
          
      // Finish
      wsc2.disconnect()      
    }    
  }
}