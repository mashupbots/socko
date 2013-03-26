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
import scala.reflect.runtime.{ universe => ru }
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.ReflectUtil
import org.mashupbots.socko.events.EndPoint

/**
 * REST operation definition. Meta data describing a REST operation.
 *
 * A REST operation is uniquely defined by its HTTP method and path.
 *
 * @param method HTTP method
 * @param rootUrl Root URL
 * @param urlTemplate relative URL template used for matching incoming REST requests
 *  - The template can be an exact match like `/pets`.
 *  - The template can have a path variable like `/pets/{petId}`. In this case, the template
 *    will match all paths with 2 segments and the first segment being `pets`. The second
 *    segment will be bound to a variable called `petId` using [[org.mashupbots.socko.rest.PathParam]].
 *  - The URL template does NOT support query string.  This is defined using
 *    [[org.mashupbots.socko.rest.QueryParam]].
 * @param processorLocatorClass Class path to singleton object that is a [[org.mashupbots.socko.rest.RestProcessorLocator]].
 *  - If empty, the assumed response class is the same class path and name as the request class;
 *    but with `Request` suffix replaced with `Processor` or `ProcessorLocator`. For `MyRestRequest`, the default 
 *    response class that will be used in is `MyRestProcessor` or `MyRestProcessorLocator`.
 * @param responseClass Class path of the response class.
 *  - If empty, the assumed response class is the same class path and name as the request class;
 *    but with `Request` suffix replaced with `Response`. For `MyRestRequest`, the default response class
 *    that will be used in is `MyRestResponse`.
 * @param name Name provided for the convenience of the UI and client code generator
 *    If empty, the name of the request class will be used without the `Request` prefix.
 * @param description Optional short description. Less than 60 characters is recommended.
 * @param notes Optional long description
 * @param depreciated Flag to indicate if this operation is depreciated or not. Defaults to `false`.
 * @param errorResponses Map of HTTP error status codes and reasons
 */
case class RestOperationDef(
  method: String,
  rootUrl: String,
  urlTemplate: String,
  processorLocatorClass: String,
  responseClass: String = "",
  name: String = "",
  description: String = "",
  notes: String = "",
  depreciated: Boolean = false,
  errorResponses: Map[Int, String] = Map.empty) extends Logger {

  private val fullUrlTemplate = if (rootUrl == "/") urlTemplate else rootUrl + urlTemplate

  /**
   * The full URL template split into path segments for ease of matching
   *
   * ==Example Usage==
   * {{{
   * // '/user/{Id}'
   * List(
   *   PathSegment("user", false),
   *   PathSegment("Id", true)
   * )
   * }}}
   *
   */
  val pathSegments: List[PathSegment] = {
    if (urlTemplate == null || urlTemplate.length == 0)
      throw new IllegalArgumentException("URI cannot be null or empty")

    val s = if (fullUrlTemplate.startsWith("/")) fullUrlTemplate.substring(1) else fullUrlTemplate
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
   * @returns `True` if the URI templates are ambiguous and 2 or more unique end points can resolve to
   *   either URI templates.  `False` otherwise..
   */
  def compareUrlTemplate(opDef: RestOperationDef): Boolean = {

    if (method != opDef.method) {
      // If different methods, then cannot be the same
      false
    } else if (pathSegments.length != opDef.pathSegments.length) {
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
          } else {
            comparePathSegment(segments.tail)
          }
        }
      }
      comparePathSegment(pathSegments.zip(opDef.pathSegments))
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
    if (method != endpoint.method) {
      false
    } else if (pathSegments.length != endpoint.pathSegments.length) {
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
      comparePathSegment(pathSegments.zip(endpoint.pathSegments))
    }
  }

}

/**
 * Companion [[org.mashupbots.socko.rest.RestOperation]] object
 */
object RestOperationDef extends Logger {

  private val restGetType = ru.typeOf[RestGet]

  private val urlTemplateName = ru.newTermName("urlTemplate")
  private val processorLocatorClassName = ru.newTermName("processorLocatorClass")
  private val responseClassName = ru.newTermName("responseClass")
  private val nameName = ru.newTermName("name")
  private val descriptionName = ru.newTermName("description")
  private val notesName = ru.newTermName("notes")
  private val depreciatedName = ru.newTermName("depreciated")
  private val errorResponsesName = ru.newTermName("errorResponses")

  /**
   * Instance a `RestDeclaration` using information of an annotation
   *
   * @param a A Rest annotation
   * @param config REST configuration
   * @returns [[org.mashupbots.socko.rest.RestDeclaration]]
   */
  def apply(a: ru.Annotation, config: RestConfig): RestOperationDef = {
    val method = if (a.tpe =:= restGetType) {
      "GET"
    } else {
      throw new IllegalStateException("Unknonw annotation type " + a.tpe.toString)
    }

    val urlTemplate = ReflectUtil.getAnnotationJavaLiteralArg(a, urlTemplateName, "")
    val processorLocatorClass = ReflectUtil.getAnnotationJavaLiteralArg(a, processorLocatorClassName, "")
    val responseClass = ReflectUtil.getAnnotationJavaLiteralArg(a, responseClassName, "")
    val name = ReflectUtil.getAnnotationJavaLiteralArg(a, nameName, "")
    val description = ReflectUtil.getAnnotationJavaLiteralArg(a, descriptionName, "")
    val notes = ReflectUtil.getAnnotationJavaLiteralArg(a, notesName, "")
    val depreciated = ReflectUtil.getAnnotationJavaLiteralArg(a, depreciatedName, false)
    val errorResponses = ReflectUtil.getAnnotationJavaStringArrayArg(a, errorResponsesName, Array.empty[String])
    val errorResponsesMap: Map[Int, String] = try {
      errorResponses.map(e => {
        val s = e.split("=")
        (Integer.parseInt(s(0).trim()), s(1).trim())
      }).toMap
    } catch {
      case ex: Throwable => {
        log.error("Error '%s' parsing error response map for '%s %s': (%s). All error responses for this operation will be ignored.".format(
          ex.getMessage, method, urlTemplate, errorResponses.mkString(",")), ex)
        Map.empty
      }
    }

    RestOperationDef(method, config.rootUrl, urlTemplate, processorLocatorClass, responseClass,
      name, description, notes, depreciated, errorResponsesMap)
  }

  /**
   * Finds if a rest annotation is in a list of annotations
   *
   * @param annotations List of annotations for a class
   * @returns The first matching rest annotation. `None` if not match
   */
  def findAnnotation(annotations: List[ru.Annotation]): Option[ru.Annotation] = {
    annotations.find(a => a.tpe =:= restGetType);
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
    else if (s.startsWith("{") && s.endsWith("}")) PathSegment(s.substring(1, s.length - 1), true)
    else PathSegment(s, false)
}

