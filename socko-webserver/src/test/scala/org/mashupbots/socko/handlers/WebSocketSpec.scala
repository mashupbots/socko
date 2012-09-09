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

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.infrastructure.WebLogFormat
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.Style
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.Props

/**
 * Basic web socket tests performed in SnoopSpec.
 *
 * These test are for more advaced web socket features
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
      case Path("/websocket/oncomplete/") => {
        wsHandshake.authorize(onComplete = Some((event: WebSocketHandshakeEvent) => {
          wsHandshake.channel.write(new TextWebSocketFrame("Hello - we have completed the handshake"))
        }))
      }
      case Path("/websocket/maxframesize/") => {
        wsHandshake.authorize(maxFrameSize = 10)
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
    
    "support callback after handshake" in {
      val wsc = new TestWebSocketClient(path + "websocket/oncomplete/")
      wsc.connect()
      wsc.isConnected should be(true)

      // Wait for message from callback to arrive 
      Thread.sleep(500)
      wsc.channelData.textBuffer.length should be > (0)
      wsc.channelData.textBuffer.toString.startsWith("Hello") should be (true)
      
      wsc.disconnect()
    }    
    
    "not connect if web socket path not found" in {
      val wsc = new TestWebSocketClient(path + "snoop/notexist/")
      wsc.connect()
      wsc.isConnected should be(false)
      wsc.disconnect()
    }
    
  }
}