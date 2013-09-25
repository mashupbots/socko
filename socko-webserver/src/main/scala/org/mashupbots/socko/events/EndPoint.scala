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

import io.netty.util.CharsetUtil
import scala.collection.JavaConversions._

import io.netty.handler.codec.http.QueryStringDecoder

/**
 * Identifies the end point associated with the firing of an event. In Socko terminology, an end point comprise
 * the request method, host, path and query string.
 *
 * @param method Web method. eg. `GET`.
 * @param host Host name or IP address
 * @param uri Request URI as per the HTTP request line. For example: `/folder/file.html` or
 *   `/folder/file.html?param1=value1&param2=value2`
 */
case class EndPoint(
  method: String,
  host: String,
  uri: String
) {

  require(method != null && method != "", "EndPoint method cannot be null or empty string")
  require(host != null && host != "", "EndPoint host cannot be null or empty string")
  require(uri != null && uri != "", "EndPoint uri cannot be null or empty string")

  lazy val isOPTIONS = method == "OPTIONS"
  lazy val isGET = method == "GET"
  lazy val isHEAD = method == "HEAD"
  lazy val isPOST = method == "POST"
  lazy val isPUT = method == "PUT"
  lazy val isDELETE = method == "DELETE"
  lazy val isTRACE = method == "TRACE"
  lazy val isCONNECT = method == "CONNECT"

  /**
   * Path portion of the request URI without the query string. For example: `/folder/file.html`
   */
  val path = if (uri.indexOf("?") < 0) uri else uri.substring(0, uri.indexOf("?"))

  /**
   * Given `/one/two/three`, `List("one", "two", "three")` is returned
   */
  lazy val pathSegments: List[String] = {
    val s = if (path.startsWith("/")) path.substring(1) else path
    s.split("/").toList
  }

  /**
   * queryString Query String without the leading "?". For example: `param1=value1&param2=value2`
   */
  val queryString = if (uri.indexOf("?") < 0) "" else uri.substring(uri.indexOf("?") + 1)

  /**
   * Provides Map access to query string parameters
   */
  lazy val queryStringMap: Map[String, List[String]] = {
    val m = new QueryStringDecoder(uri, CharsetUtil.UTF_8).parameters.toMap
    // Map the Java list values to Scala list
    m.map { case (key, value) => (key, value.toList) }
  }

  /**
   * Returns the query string value with the specified name.  If there are
   * more than one value, the first value is returned.
   *
   * @return `Some(String)` or `None` if there is no such name
   */
  def getQueryString(name: String): Option[String] = {
    try {
      val v = queryStringMap(name)
      if (v == null)
        None
      else
        Some(v.get(0))
    } catch {
      case ex: NoSuchElementException => None
    }
  }
}