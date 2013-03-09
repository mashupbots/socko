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

import scala.collection.JavaConversions._

/**
 * Declaration for a REST operation
 */
trait RestDeclaration {

  /**
   * HTTP method
   */
  def method: String

  /**
   * URI template used for matching incoming REST requests
   *
   * ==Exact Match==
   * The template can be an exact match like `/pets`.
   *
   * ==Variable Match==
   * The template can have a path variable like `/pets/{petId}`. In this case, the template
   * will match all paths with 2 segments and the first segment being `pets`. The second
   * segment will be bound to a variable called `petId` using [[org.mashupbots.socko.rest.PathParam]].
   *
   * ==No Query String==
   * The URI template does NOT support query string.
   * That is defined using [[org.mashupbots.socko.rest.QueryParam]].
   *
   */
  def uriTemplate: String

  /**
   * Path to actor to which this request will be sent for processing.
   *
   * You can also bind your request to an actor at bootup time using an actor path of `lookup:{key}`.
   * The `key` is the key to a map of actor names passed into [[org.mashupbots.socko.rest.RestRegistry]].
   */
  def actorPath: String

  /**
   * Class path of the response class.
   *
   * If empty, the assumed response class is the same class path and name as the request class;
   * but with `Request` suffix replaced with `Response`.
   *
   * For `MyRestRequest`, the default response class would be `MyRestResponse`.
   */
  def responseClass: String

  /**
   * Short name provided for the convenience of the UI and client code generator
   */
  def name: String

  /**
   * Short description. Less than 60 characters is recommended.
   */
  def description: String

  /**
   * Optional long description
   */
  def notes: String

  /**
   * Flag to indicate if this endpoint is depreciated or not. Defaults to `false`.
   */
  def depreciated: Boolean

  /**
   * map of error codes and descriptions
   */
  def errorResponses: Map[String, String]

  /**
   * The `uriTemplate` split into path segments for ease of matching
   */
  val pathSegments: List[PathSegment] = {
    if (uriTemplate == null || uriTemplate.length == 0)
      throw new IllegalArgumentException("URI cannot be null or empty")

    val s = if (uriTemplate.startsWith("/")) uriTemplate.substring(1) else uriTemplate
    val ss = s.split("/").toList
    val segments = ss.map(s => PathSegment(s))
    segments
  }
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
    else if (s.startsWith("{") && s.endsWith("}")) PathSegment(s.substring(1, s.length - 2), true)
    else PathSegment(s, false)
}

