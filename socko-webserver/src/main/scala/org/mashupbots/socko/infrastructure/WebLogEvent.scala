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
package org.mashupbots.socko.infrastructure

import java.net.InetSocketAddress
import java.net.SocketAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * A web log event to record
 *
 * @param timestamp Timestamp for the event
 * @param serverName Socko Web Server instance name
 * @param channelId Netty channel id 
 * @param clientAddress Client's socket address. We don't convert to string here in case JDK performs a blocknig
 *  reverse DNS lookup.
 * @param serverAddress Server's socket address. We don't convert to string here in case JDK performs a blocknig
 *  reverse DNS lookup.
 * @param username Authenticated user name
 * @param method The action the client was trying to perform (for example, a GET method).
 * @param uri The resource accessed; for example, Default.htm.
 * @param requestSize The number of bytes received by the server.
 * @param responseStatusCode The status of the action, in HTTP or FTP terms.
 * @param responseSize The number of bytes sent by the server.
 * @param timeTaken The duration of time, in milliseconds, that this action consumed.
 * @param protocolVersion The protocol (HTTP, FTP) version used by the client. For HTTP this will be either
 *   HTTP/1.0 or HTTP/1.1.
 * @param userAgent The browser used on the client.
 * @param referrer The previous site visited by the user. This site provided a link to the current site.
 */
case class WebLogEvent(
  timestamp: Date,
  serverName: String,
  channelId: Int,  
  clientAddress: SocketAddress,
  serverAddress: SocketAddress,
  username: Option[String],
  method: String,
  uri: String,
  requestSize: Long,
  responseStatusCode: Int,
  responseSize: Long,
  timeTaken: Long,
  protocolVersion: String,
  userAgent: Option[String],
  referrer: Option[String]) {

  /**
   * Creates a log entry in the [[http://en.wikipedia.org/wiki/Common_Log_Format common log format]].
   *
   * {{{
   * 216.67.1.91 - leon [01/Jul/2002:12:11:52 +0000] "GET /index.html HTTP/1.1" 200 431
   * }}}
   */
  def toCommonFormat(): String = {
    val inetClientAddress = clientAddress.asInstanceOf[InetSocketAddress]
    val sf = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z")
    sf.setTimeZone(WebLogEvent.UTC_TZ)
    val sb = new StringBuilder
    
    // Client address
    sb.append(inetClientAddress.getAddress.getHostAddress)
    sb.append(" - ")

    // Username
    sb.append(if (username.isDefined) removeWhitespace(username.get) else "-")
    sb.append(" [")
    
    // Timestamp
    sb.append(sf.format(timestamp))
    sb.append("] \"")

    // Request Line
    sb.append(removeWhitespace(method))
    sb.append(" ")
    sb.append(removeWhitespace(uri))
    sb.append(" ")
    sb.append(removeWhitespace(protocolVersion))
    sb.append("\" ")

    // Status
    sb.append(responseStatusCode)
    sb.append(" ")
    
    // Response size
    sb.append(responseSize)
    
    // Done
    sb.toString
  }
  
  /**
   * Creates a log entry in the [[http://httpd.apache.org/docs/1.3/logs.html combined log format]].
   *
   * {{{
   * 216.67.1.91 - leon [01/Jul/2002:12:11:52 +0000] "GET /index.html HTTP/1.1" 200 431 "http://www.loganalyzer.net/" "Mozilla/4.05 [en] (WinNT; I)"
   * }}}
   */
  def toCombinedFormat(): String = {
    val inetClientAddress = clientAddress.asInstanceOf[InetSocketAddress]
    val sf = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z")
    sf.setTimeZone(WebLogEvent.UTC_TZ)
    val sb = new StringBuilder
    
    // Client address
    sb.append(inetClientAddress.getAddress.getHostAddress)
    sb.append(" - ")

    // Username
    sb.append(if (username.isDefined) removeWhitespace(username.get) else "-")
    sb.append(" [")
    
    // Timestamp
    sb.append(sf.format(timestamp))
    sb.append("] \"")

    // Request Line
    sb.append(removeWhitespace(method))
    sb.append(" ")
    sb.append(removeWhitespace(uri))
    sb.append(" ")
    sb.append(removeWhitespace(protocolVersion))
    sb.append("\" ")

    // Status
    sb.append(responseStatusCode)
    sb.append(" ")
    
    // Response size
    sb.append(responseSize)
    sb.append(" ")

    //Referrer - because this is quoted, no need to remove whitespace
    sb.append(if (referrer.isDefined) "\"" + referrer.get + "\"" else "-")
    sb.append(" ")

    //User-Agent - because this is quoted, no need to remove whitespace
    sb.append(if (userAgent.isDefined) "\"" + userAgent.get + "\"" else "-")
    
    // Done
    sb.toString
  }
  
  /**
   * Creates a log entry in the [[http://www.w3.org/TR/WD-logfile.html extended log format]].
   *
   * {{{
   * #Software: Socko
   * #Version: 1.0
   * #Date: 2002-05-02 17:42:15
   * #Fields: date time c-ip cs-username s-ip s-port cs-method cs-uri-stem cs-uri-query sc-status sc-bytes cs-bytes time-taken cs(User-Agent) cs(Referrer)
   * 2002-05-24 20:18:01 172.224.24.114 - 206.73.118.24 80 GET /Default.htm - 200 7930 248 31 Mozilla/4.0+(compatible;+MSIE+5.01;+Windows+2000+Server) http://64.224.24.114/
   * }}}
   */
  def toExtendedFormat(): String = {
    val inetClientAddress = clientAddress.asInstanceOf[InetSocketAddress]
    val inetServerAddress = serverAddress.asInstanceOf[InetSocketAddress]
    val sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    sf.setTimeZone(WebLogEvent.UTC_TZ)
    val sb = new StringBuilder
    
    // date time
    sb.append(sf.format(timestamp))
    sb.append(" ")
    
    // c-ip
    sb.append(inetClientAddress.getAddress.getHostAddress)
    sb.append(" ")

    // c-username
    sb.append(if (username.isDefined) removeWhitespace(username.get) else "-")
    sb.append(" ")
    
    // s-ip
    sb.append(inetServerAddress.getAddress.getHostAddress)
    sb.append(" ")

    // s-port
    sb.append(inetServerAddress.getPort)
    sb.append(" ")

    //cs-method
    sb.append(removeWhitespace(method))
    sb.append(" ")
    
    //cs-uri-stem cs-uri-query
    val idx = uri.indexOf("?")
    val uriStem = if (idx < 0) uri else uri.substring(0, uri.indexOf("?"))
    val uriQuery = if (idx < 0) "-" else removeWhitespace(uri.substring(uri.indexOf("?") + 1))    
    sb.append(removeWhitespace(uriStem))
    sb.append(" ")
    sb.append(uriQuery)
    sb.append(" ")

    //sc-status
    sb.append(responseStatusCode)
    sb.append(" ")

    //sc-bytes
    sb.append(responseSize)
    sb.append(" ")

    //cs-bytes
    sb.append(requestSize)
    sb.append(" ")

    //time-taken
    sb.append(timeTaken)
    sb.append(" ")

    //cs(User-Agent)
    sb.append(if (userAgent.isDefined) removeWhitespace(userAgent.get) else "-")
    sb.append(" ")

    //cs(Referrer)
    sb.append(if (referrer.isDefined) removeWhitespace(referrer.get) else "-")
    
    // Done
    sb.toString
  }
  
  private def removeWhitespace(s: String): String = {
    s.replaceAll("\\s", "+")
  }
  
}

object WebLogEvent {
  val UTC_TZ = TimeZone.getTimeZone("UTC")
}

