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
//
package org.mashupbots.socko.rest

import org.mashupbots.socko.events.EndPoint

/**
 * The HTTP method and path to a REST operation
 *
 * @param method HTTP method. e.g. `GET`.
 * @param rootPath Root path of the REST service. e.g. `/api`.
 * @param relativePath Path relative to the `rootPath` for the REST operation
 */
case class RestEndPoint(
  method: String,
  rootPath: String,
  relativePath: String) {

  /**
   * Full path from combining the `rootPath` with `relativePath`.
   */
  val fullPath = if (rootPath == "/") relativePath else {
    rootPath + (if (relativePath.startsWith("/")) "" else "/") + relativePath
  }

  /**
   * The full path split into path segments for ease of matching
   *
   * ==Example Usage==
   * If path = `/user/{Id}` and rootUrl in the config is `/api`, the full path segment are
   *
   * {{{
   * List(
   *   PathSegment("api", false),
   *   PathSegment("user", false),
   *   PathSegment("Id", true)
   * )
   * }}}
   */
  val fullPathSegments: List[PathSegment] = {
    if (relativePath == null || relativePath.length == 0)
      throw new IllegalArgumentException("Declaration path cannot be null or empty")

    val s = if (fullPath.startsWith("/")) fullPath.substring(1) else fullPath
    val ss = s.split("/").toList
    val segments = ss.map(s => PathSegment(s))
    segments
  }

  /**
   * Number of static path segments. Used to determine best match
   */
  val staticFullPathSegementsCount: Int = fullPathSegments.count(ps => !ps.isVariable)

  /**
   * The relative URL template split into path segments for ease of matching
   *
   * ==Example Usage==
   * If path = `/user/{Id}` and rootUrl in the config is `/api`, the relative path segment are
   *
   * {{{
   * List(
   *   PathSegment("user", false),
   *   PathSegment("Id", true)
   * )
   * }}}
   */
  val relativePathSegments: List[PathSegment] = {
    if (relativePath == null || relativePath.length == 0)
      throw new IllegalArgumentException("URI cannot be null or empty")

    val s = if (relativePath.startsWith("/")) relativePath.substring(1) else relativePath
    val ss = s.split("/").toList
    val segments = ss.map(s => PathSegment(s))
    segments
  }

  /**
   * Compares the URL of this operation to another.
   *
   * Comparison is based on method and path segments.
   *
   * For example, `GET /pets/{id}` is the same as `GET /{type}/{id}` because `{type}` is a variable
   * and can contain `pets`.
   *
   * However, the following are different:
   *  - `GET /pets` is different to `GET /users` because the static paths are different
   *  - `DELETE /pets/{id}` is different to `PUT /pets/{id}` because methods are different
   *
   * @param op Another REST operation to compare against
   * @return `True` if the URI templates are ambiguous and 2 or more unique end points can resolve to
   *   either URI templates.  `False` otherwise..
   */
  def comparePath(restEndPoint: RestEndPoint): Boolean = {

    if (method != restEndPoint.method) {
      // If different methods, then cannot be the same
      false
    } else if (fullPathSegments.length != restEndPoint.fullPathSegments.length) {
      // If different number of segments, then cannot be the same
      return false
    } else {
      // Compare paths
      def comparePathSegment(segments: List[(PathSegment, PathSegment)]): Boolean = {
        if (segments.isEmpty) {
          // Must resolve to the same endpoint - same method and path segments
          true
        } else {
          val (l, r) = segments.head
          if (!l.isVariable && !r.isVariable && l.name != r.name) {
            // If static segments are different, then cannot be the same
            false
          } else if ((l.isVariable && !r.isVariable) || (!l.isVariable && r.isVariable)) {
            // If mix of static and variable, then assume different. The static will take precedence
            false
          } else {
            // If both variable, then assume same so go to the next level
            comparePathSegment(segments.tail)
          }
        }
      }
      comparePathSegment(fullPathSegments.zip(restEndPoint.fullPathSegments))
    }
  }

  /**
   * Compares the URL template with the specified end point.
   *
   * For example, `GET /pets/{id}` matches the end point `GET /pets/123`.
   *
   * @param endpoint End point to match
   * @return `True` if this is a match; `False` if not a match.
   */
  def matchEndPoint(endpoint: EndPoint): Boolean = {
    // Convert HEAD to GET
    val endpointMethod = if (endpoint.isHEAD) "GET" else endpoint.method

    if (method != endpointMethod) {
      false
    } else if (fullPathSegments.length != endpoint.pathSegments.length) {
      return false
    } else {
      // Compare paths
      def comparePathSegment(segments: List[(PathSegment, String)]): Boolean = {
        if (segments.isEmpty) {
          // Must resolve to the same endpoint - same method and path segments
          true
        } else {
          val (ps, endpoint) = segments.head
          if (!ps.isVariable && ps.name != endpoint) {
            // If static segments are different, then cannot be the same
            false
          } else {
            comparePathSegment(segments.tail)
          }
        }
      }
      comparePathSegment(fullPathSegments.zip(endpoint.pathSegments))
    }
  }

}

/**
 * Companion object
 */
object RestEndPoint {

  /**
   * Alternative constructor using configuration and registration
   *
   * @param config REST configuration
   * @param registration REST registration details
   */
  def apply(config: RestConfig, registration: RestRegistration): RestEndPoint =
    RestEndPoint(registration.method.toString, config.rootPath, registration.path)
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
    else if (s.startsWith("{") && s.endsWith("}")) PathSegment(s.substring(1, s.length - 1), true)
    else PathSegment(s, false)
}

