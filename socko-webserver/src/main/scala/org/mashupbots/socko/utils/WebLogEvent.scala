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
package org.mashupbots.socko.utils

import java.util.Date
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpRequest
import java.net.InetSocketAddress
import java.net.SocketAddress
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders


/**
 * A web log event to record
 *
 * @param timestamp Timestamp for the event
 * @param clientAddress Client's socket address. We don't convert to string here in case JDK performs a blocknig
 *  reverse DNS lookup.
 * @param serverAddress Server's socket address. We don't convert to string here in case JDK performs a blocknig
 *  reverse DNS lookup.
 * @param username Authenticated user naem
 * @param method The action the client was trying to perform (for example, a GET method).
 * @param uri The resource accessed; for example, Default.htm.
 * @param responseStatusCode The status of the action, in HTTP or FTP terms.
 * @param responseSize The number of bytes sent by the server.
 * @param requestSize The number of bytes received by the server.
 * @param timeTaken The duration of time, in milliseconds, that this action consumed.
 * @param protocolVersion The protocol (HTTP, FTP) version used by the client. For HTTP this will be either
 *   HTTP/1.0 or HTTP/1.1.
 * @param userAgent The browser used on the client.
 * @param referrer The previous site visited by the user. This site provided a link to the current site.
 */
case class WebLogEvent(
  timestamp: Date,
  clientAddress: SocketAddress,
  serverAddress: SocketAddress,
  username: Option[String],
  method: String,
  uri: String,
  responseStatusCode: Int,
  responseSize: Long,
  requestSize: Long,
  timeTaken: Long,
  protocolVersion: String,
  userAgent: Option[String],
  referrer: Option[String]) {
}

object WebLogEvent {
  /**
   * Reads an optional HTTP header value
   */
  def getHttpHeader(request: HttpRequest, name: String): Option[String] = {
    val v = request.getHeader(name)
    if (v == null || v == "") None else Some(v)
  }
  
  /**
   * Returns the path portion of a URI string
   * 
   * @parm uri URI
   */
  def getUriStem(uri: String): String = {
    if (uri.indexOf("?") < 0) uri else uri.substring(0, uri.indexOf("?"))
  }
  
  /**
   * Returns the query string portion of a URI string
   * 
   * @parm uri URI
   */
  def getUriQuery(uri: String): Option[String] = {
    if (uri.indexOf("?") < 0) None else Some(uri.substring(uri.indexOf("?")))
  }
  
}
