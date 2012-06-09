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

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.ChannelLocal
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType
import org.jboss.netty.handler.codec.http.multipart.Attribute
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import org.jboss.netty.handler.codec.http.multipart.FileUpload
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import org.mashupbots.socko.events.HttpChunkEvent
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.WebSocketFrameEvent

import akka.actor.Actor
import akka.event.Logging

/**
 * Sends a response containing information about the received event.
 * 
 * We use this in our testing of Socko
 */
class SnoopHandler extends Actor {
  private val log = Logging(context.system, this)

  /**
   * Process incoming events
   */
  def receive = {
    case httpRequestEvent: HttpRequestEvent =>
      snoopHttpRequest(httpRequestEvent)
      context.stop(self)
    case httpChunkEvent: HttpChunkEvent =>
      snoopHttpChunk(httpChunkEvent)
      context.stop(self)
    case webSocketEvent: WebSocketFrameEvent =>
      snoopWebSocket(webSocketEvent)
      context.stop(self)
    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }

  /**
   * Echo the details of the HTTP request that we just received
   */
  private def snoopHttpRequest(event: HttpRequestEvent) {
    val channel = event.channel
    val request = event.nettyHttpRequest

    // Send 100 continue if required
    if (event.request.is100ContinueExpected) {
      event.response.write100Continue()
    }

    val buf = new StringBuilder()
    buf.append("Socko Snoop Processor\r\n")
    buf.append("=====================\r\n")

    buf.append("VERSION: " + request.getProtocolVersion + "\r\n")
    buf.append("METHOD: " + event.endPoint.method + "\r\n")
    buf.append("HOSTNAME: " + event.endPoint.host + "\r\n")
    buf.append("REQUEST_URI: " + event.endPoint.path + "\r\n\r\n")

    val headers = request.getHeaders().toList
    headers.foreach(h => buf.append("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n"))

    val params = event.endPoint.queryStringMap
    if (!params.isEmpty) {
      params.foreach({
        case (key, values) => {
          values.foreach(v => buf.append("QUERYSTRING PARAM: " + key + " = " + v + "\r\n"))
        }
      })
      buf.append("\r\n")
    }

    // If post, then try to parse the data
    val contentType = event.request.contentType
    if (contentType != "" &&
      (contentType.startsWith("multipart/form-data") ||
        contentType.startsWith("application/x-www-form-urlencoded"))) {
      buf.append("FORM DATA\r\n")
      val decoder = new HttpPostRequestDecoder(HttpDataFactory.value, event.nettyHttpRequest)
      val dataList = decoder.getBodyHttpDatas().toList
      dataList.foreach(data => {
        if (data.getHttpDataType() == HttpDataType.Attribute) {
          // Normal post data
          val attribute = data.asInstanceOf[Attribute]
          buf.append("  " + data.getName + "=" + attribute.getValue + "\r\n")
        } else if (data.getHttpDataType() == HttpDataType.FileUpload) {
          // File upload
          val fileUpload = data.asInstanceOf[FileUpload]
          buf.append("  File Field=" + fileUpload.getName + "\r\n")
          buf.append("  File Name=" + fileUpload.getFilename + "\r\n")
          buf.append("  File MIME Type=" + fileUpload.getContentType + "\r\n")
          buf.append("  File Content=" + fileUpload.getString(fileUpload.getCharset) + "\r\n")
        }
      })
    } else {
      val content = event.request.content.toString()
      if (content.length > 0) {
        buf.append("CONTENT: " + content + "\r\n")
      }
    }

    val x = event.response
    val y = event.response
    
    log.info("HttpRequest: " + buf.toString)
    event.response.write(buf.toString)
  }

  /**
   * Echo the details of the HTTP chunk that we just received
   *
   * This will not be called unless chunk aggregation is turned off
   */
  private def snoopHttpChunk(event: HttpChunkEvent) {
    // Accumulate chunk info in a string buffer stored in the channel
    val channel = event.channel
    var buf = ChunkDataStore.data.get(channel)
    if (buf == null) {
      buf = new StringBuilder()
      ChunkDataStore.data.set(channel, buf)
    }

    if (event.chunk.isLastChunk) {
      buf.append("END OF CONTENT\r\n")
      event.chunk.trailingHeaders.foreach(h => buf.append("HEADER: " + h._1 + " = " + h._2 + "\r\n"))
      buf.append("\r\n")

      log.info("HttpChunk: " + buf.toString)
      event.response.write(buf.toString)
    } else {
      buf.append("CHUNK: " + event.chunk.toString + "\r\n")
    }
  }

  /**
   * Echo the details of the web socket frame that we just received
   */
  private def snoopWebSocket(event: WebSocketFrameEvent) {
    if (event.isText) {
      log.info("TextWebSocketFrame: " + event.readText)
      event.writeText(event.readText)
    } else if (event.isBinary) {
      log.info("BinaryWebSocketFrame")
      event.writeBinary(event.readBinary)
    }
  }

}

/**
 * Store of channel specific data
 */
object ChunkDataStore {
  val data = new ChannelLocal[StringBuilder]
}

object HttpDataFactory {
  // Disk if size exceed MINSIZE
  val value = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE)
}
