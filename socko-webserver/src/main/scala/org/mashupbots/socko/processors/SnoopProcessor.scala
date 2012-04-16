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

package org.mashupbots.socko.processors

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.http.HttpHeaders
import org.jboss.netty.channel.ChannelLocal
import org.mashupbots.socko.context.HttpChunkProcessingContext
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.context.WsProcessingContext
import org.mashupbots.socko.postdecoder.InterfaceHttpData.HttpDataType
import org.mashupbots.socko.postdecoder.Attribute
import org.mashupbots.socko.postdecoder.DefaultHttpDataFactory
import org.mashupbots.socko.postdecoder.FileUpload
import org.mashupbots.socko.postdecoder.HttpPostRequestDecoder

import akka.actor.Actor
import akka.event.Logging

/**
 * A processor that send a response containing information about the request.
 * 
 * We use this for our testing.
 */
class SnoopProcessor extends Actor {
  val log = Logging(context.system, this)

  /**
   * Process incoming messages
   */
  def receive = {
    case httpRequestContext: HttpRequestProcessingContext =>
      snoopHttpRequest(httpRequestContext)
      context.stop(self)
    case httpChunkContext: HttpChunkProcessingContext =>
      snoopHttpChunk(httpChunkContext)
      context.stop(self)
    case webSocketContext: WsProcessingContext =>
      snoopWebSocket(webSocketContext)
      context.stop(self)
    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }

  /**
   * Echo out details of the HTTP request that we just received
   */
  private def snoopHttpRequest(ctx: HttpRequestProcessingContext) {
    val channel = ctx.channel
    val request = ctx.httpRequest

    // Send 100 continue if required
    if (ctx.is100ContinueExpected) {
      ctx.write100Continue()
    }

    val buf = new StringBuilder()
    buf.append("Socko Snoop Processor\r\n")
    buf.append("=====================\r\n")

    buf.append("VERSION: " + request.getProtocolVersion + "\r\n")
    buf.append("METHOD: " + ctx.endPoint.method + "\r\n")
    buf.append("HOSTNAME: " + ctx.endPoint.host + "\r\n")
    buf.append("REQUEST_URI: " + ctx.endPoint.path + "\r\n\r\n")

    val headers = request.getHeaders().toList
    headers.foreach(h => buf.append("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n"))

    val params = ctx.endPoint.queryStringMap
    if (!params.isEmpty) {
      params.foreach({
        case (key, values) => {
          values.foreach(v => buf.append("QUERYSTRING PARAM: " + key + " = " + v + "\r\n"))
        }
      })
      buf.append("\r\n")
    }

    // If post, then try to parse the data
    val contentType = ctx.getHeader(HttpHeaders.CONTENT_TYPE)
    if (contentType.isDefined &&
      (contentType.get.startsWith("multipart/form-data") ||
        contentType.get.startsWith("application/x-www-form-urlencoded"))) {
      buf.append("FORM DATA\r\n")
      val decoder = new HttpPostRequestDecoder(HttpDataFactory.value, ctx.httpRequest)
      val datas = decoder.getBodyHttpDatas().toList
      datas.foreach(data => {
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
      val content = ctx.readStringContent
      if (content.length > 0) {
        buf.append("CONTENT: " + content + "\r\n")
      }
    }

    log.info("HttpRequest: " + buf.toString)
    ctx.writeResponse(buf.toString)
  }

  /**
   * Echo out details of the HTTP chunk that we just received
   *
   * This will not be called unless chunk aggregation is turned off
   */
  private def snoopHttpChunk(ctx: HttpChunkProcessingContext) {
    // Accumulate chunk info in a string buffer stored in the channel
    val channel = ctx.channel
    var buf = ChunkDataStore.data.get(channel)
    if (buf == null) {
      buf = new StringBuilder()
      ChunkDataStore.data.set(channel, buf)
    }

    if (ctx.isLastChunk) {
      buf.append("END OF CONTENT\r\n")
      ctx.lastChunkHeaders.foreach(h => buf.append("HEADER: " + h.getKey + " = " + h.getValue + "\r\n"))
      buf.append("\r\n")

      log.info("HttpChunk: " + buf.toString)
      ctx.writeResponse(buf.toString)
    } else {
      buf.append("CHUNK: " + ctx.readStringContent + "\r\n")
    }
  }

  /**
   * Echo out details of the web socket frame that we just received
   */
  private def snoopWebSocket(ctx: WsProcessingContext) {
    if (ctx.isText) {
      log.info("TextWebSocketFrame: " + ctx.readStringContent)
      ctx.writeText(ctx.readStringContent)
    } else if (ctx.isBinary) {
      log.info("BinaryWebSocketFrame")
      ctx.writeBinaryData(ctx.readBinaryContent)
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
