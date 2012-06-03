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

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpRequest
import org.mashupbots.socko.infrastructure.WebLogEvent

import akka.actor.actorRef2Scala

/**
 * Event fired when a HTTP request has been received
 *
 * @param channel Channel by which the request entered and response will be written
 * @param nettyHttpRequest Incoming Netty request for processing
 * @param config Processing configuration
 */
case class HttpRequestEvent(
  channel: Channel,
  nettyHttpRequest: HttpRequest,
  config: HttpEventConfig) extends HttpEvent {

  /**
   * Incoming HTTP request
   */
  val request = CurrentHttpRequestMessage(nettyHttpRequest)

  /**
   * Outgoing HTTP Response
   */
  val response = HttpResponseMessage(this)

  /**
   * HTTP End point for this request
   */
  val endPoint = request.endPoint

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
      request.endPoint.method,
      request.endPoint.uri,
      request.contentLength,
      responseStatusCode,
      responseSize,
      duration,
      request.httpVersion,
      request.headers.get(HttpHeaders.Names.USER_AGENT),
      request.headers.get(HttpHeaders.Names.REFERER))
  }
}
