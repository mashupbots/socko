//
// Copyright 2013 Vibul Imtarnasan, David Bolton and Socko contributors.
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
package org.mashupbots.socko.rest

import akka.actor.Actor
import akka.event.Logging
import org.mashupbots.socko.events.HttpRequestEvent

/**
 * Encapsulates a REST endpoint that we can use to match incoming requests
 */
case class RestEndPoint(
  method: String,
  pathSegements: Array[PathSegment],
  actorPath: String) {

}

/**
 * Encapsulates a path segment
 *
 * ==Example Usage==
 * {{{
 * // '{Id}'
 * PathSegment("Id", true)
 * 
 * // 'user'
 * PathSegment("user", false)
 * }}}
 * 
 * @param name Name of the variable or static segment 
 * @param isVariable Flag to denote if this segment is variable and is intended to be bound to a variable or not.
 *   If not, it is a static segment
 */
case class PathSegment(
  name: String,
  isVariable: Boolean) {

  /**
   * A path segment matches if either path segments are variable, or if
   * both path segments are not variable, then the names must match
   */
  def pathMatch(obj: Any): Boolean = {
    obj match {
      case p: PathSegment => {
        if (!isVariable && !p.isVariable) name == name
        else true
      }
      case _ => false
    }
  }
}

/**
 * Factory to parse a string into a path segment
 */
object PathSegment {

  /**
   * Parses a string into a path segment
   *
   * A string is a variable if it is in the format: `{name}`.  The `name` part will be put in the
   * name field of the path segment
   *
   * @param s string to parse
   */
  def apply(s: String): PathSegment =
    if (s == null || s.length == 0) throw new IllegalArgumentException("Path segment cannot be null or empty")
    else if (s.startsWith("{") && s.endsWith("}")) PathSegment(s.substring(1, s.length - 2), true)
    else PathSegment(s, false)
}

/**
 * Factory to parse a URI into its path segments
 */
object PathSegments {

  /**
   * Parses a URI into its path segments
   *
   * @param uri URI to parse
   */
  def apply(uri: String): Array[PathSegment] = {
    if (uri == null || uri.length == 0)
      throw new IllegalArgumentException("URI cannot be null or empty")

    val s = if (uri.startsWith("/")) uri.substring(1) else uri
    val ss = s.split("/")
    val segments = ss.map(s => PathSegment(s))
    segments
  }
}
