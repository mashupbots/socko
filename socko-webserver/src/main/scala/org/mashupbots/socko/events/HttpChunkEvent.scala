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
package org.mashupbots.socko.events

import java.nio.charset.Charset
import scala.collection.JavaConversions.asScalaBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpChunkTrailer
import org.jboss.netty.util.CharsetUtil
import org.mashupbots.socko.infrastructure.WebLogEvent
import java.util.Date
import org.jboss.netty.handler.codec.http.HttpHeaders

/**
 * Event fired when a HTTP chunks is received
 *
 * The [[org.mashupbots.socko.events.HttpChunkEvent]] will only be fired if:
 *  - if the web server is configured NOT to aggregate chunks
 *  - after an initial [[org.mashupbots.socko.events.HttpRequestEvent]] has been received where the
 *    `isChunked` property is set to `True`.
 *
 * @param channel Channel by which the request entered and response will be written
 * @param initialHttpRequest The initial HTTP request associated with this chunk
 * @param nettyHttpChunk Incoming chunk of data for processing
 * @param config Processing configuration
 */
case class HttpChunkEvent(
  channel: Channel,
  initialHttpRequest: InitialHttpRequestMessage,
  nettyHttpChunk: HttpChunk,
  config: HttpEventConfig) extends HttpEvent {

  /**
   * Data associated with this chunk
   */
  val chunk: HttpChunkMessage = HttpChunkMessage(nettyHttpChunk)

  /**
   * Outgoing HTTP Response
   *
   * Typically, the response should only be written if this is the last chunk. However, in the event of an error,
   * you may wish to try to send a response back to the client.
   */
  val response = HttpResponseMessage(this)

  /**
   * HTTP End point for this request
   */
  val endPoint = initialHttpRequest.endPoint

  /**
   * Adds an entry to the web log
   *
   * If you have an authenticated user, be sure to set `this.username` before writing a web log.
   *
   * @param responseStatusCode HTTP status code
   * @param responseSize length of response content in bytes
   */
  def writeWebLog(responseStatusCode: Int, responseSize: Long) {
    if (config.webLogWriter.isEmpty) {
      return
    }

    config.webLogWriter.get ! WebLogEvent(
      this.createdOn,
      config.serverName,
      channel.getId,
      channel.getRemoteAddress,
      channel.getLocalAddress,
      username,
      initialHttpRequest.endPoint.method,
      initialHttpRequest.endPoint.uri,
      initialHttpRequest.totalChunkContentLength,
      responseStatusCode,
      responseSize,
      initialHttpRequest.duration,
      initialHttpRequest.httpVersion,
      initialHttpRequest.headers.get(HttpHeaders.Names.USER_AGENT),
      initialHttpRequest.headers.get(HttpHeaders.Names.REFERER))
  }

}