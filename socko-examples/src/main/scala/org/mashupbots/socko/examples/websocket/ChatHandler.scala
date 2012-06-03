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
package org.mashupbots.socko.examples.websocket

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.mashupbots.socko.handlers.WebSocketBroadcastText

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.event.Logging

/**
 * Web Socket processor for chatting
 */
class ChatHandler extends Actor {
  val log = Logging(context.system, this)

  /**
   * Process incoming events
   */
  def receive = {
    case event: HttpRequestEvent =>
      // Return the HTML page to setup web sockets in the browser
      writeHTML(event)
      context.stop(self)
    case event: WebSocketFrameEvent =>
      // Echo web socket text frames
      writeWebSocketResponse(event)
      context.stop(self)
    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }

  /**
   * Write HTML page to setup a web socket on the browser
   */
  private def writeHTML(ctx: HttpRequestEvent) {
    // Send 100 continue if required
    if (ctx.request.is100ContinueExpected) {
      ctx.response.write100Continue()
    }

    val buf = new StringBuilder()
    buf.append("<html><head><title>Socko Web Socket Example</title></head>\n")
    buf.append("<body>\n")
    buf.append("<script type=\"text/javascript\">\n")
    buf.append("  var socket;\n")
    buf.append("  if (!window.WebSocket) {\n")
    buf.append("    window.WebSocket = window.MozWebSocket;\n")
    buf.append("  }\n")
    buf.append("  if (window.WebSocket) {\n")
    buf.append("    socket = new WebSocket(\"ws://localhost:8888/websocket/\");\n") // Note the address must match the route
    buf.append("    socket.onmessage = function(event) { var ta = document.getElementById('responseText'); ta.value = ta.value + '\\n' + event.data };\n")
    buf.append("    socket.onopen = function(event) { var ta = document.getElementById('responseText'); ta.value = \"Web Socket opened!\"; };\n")
    buf.append("    socket.onclose = function(event) { var ta = document.getElementById('responseText'); ta.value = ta.value + \"Web Socket closed\"; };\n")
    buf.append("  } else { \n")
    buf.append("    alert(\"Your browser does not support Web Sockets.\");\n")
    buf.append("  }\n")
    buf.append("  \n")
    buf.append("  function send(message) {\n")
    buf.append("    if (!window.WebSocket) { return; }\n")
    buf.append("    if (socket.readyState == WebSocket.OPEN) {\n")
    buf.append("      socket.send(message);\n")
    buf.append("    } else {\n")
    buf.append("      alert(\"The socket is not open.\");\n")
    buf.append("    }\n")
    buf.append("  }\n")
    buf.append("</script>\n")
    buf.append("<h1>Socko Web Socket Chat Example</h1>\n")
    buf.append("<form onsubmit=\"return false;\">\n")
    buf.append("  <input type=\"text\" name=\"message\" value=\"Hello, World!\"/>\n")
    buf.append("  <input type=\"button\" value=\"Chat\" onclick=\"send(this.form.message.value)\" />\n")
    buf.append("  \n")
    buf.append("  <h3>Output</h3>\n")
    buf.append("  <textarea id=\"responseText\" style=\"width: 500px; height:300px;\"></textarea>\n")
    buf.append("</form>\n")
    buf.append("</body>\n")
    buf.append("</html>\n")

    ctx.response.write(buf.toString, "text/html; charset=UTF-8")
  }

  /**
   * Echo the details of the web socket frame that we just received; but in upper case.
   */
  private def writeWebSocketResponse(event: WebSocketFrameEvent) {
    log.info("TextWebSocketFrame: " + event.readText)

    val dateFormatter = new SimpleDateFormat("HH:mm:ss")
    val time = new GregorianCalendar()
    val ts = dateFormatter.format(time.getTime())

    val broadcaster = context.actorFor("/user/webSocketBroadcaster")
    broadcaster ! WebSocketBroadcastText(ts + " " + event.readText)
  }

}

