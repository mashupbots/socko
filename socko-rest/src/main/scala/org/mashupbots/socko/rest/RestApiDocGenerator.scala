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

import org.mashupbots.socko.infrastructure.Logger
import scala.collection.mutable.HashMap
import org.json4s.native.{ Serialization => json }
import org.json4s.NoTypeHints
import org.mashupbots.socko.infrastructure.CharsetUtil

/**
 * Generates API documentation
 */
object RestApiDocGenerator extends Logger {

  /**
   * URL path relative to the config `rootUrl` that will trigger the return of the API documentation
   */
  val urlPath = "/api-docs"

  /**
   * Generates a Map of URL paths and the associated API documentation to be returned for these paths
   *
   * @param operations Rest operations
   * @param config Rest configuration
   * @returns Map with the `key` being the exact path to match, the value is the `UTF-8` encoded response
   */
  def generate(operations: Seq[RestOperation], config: RestConfig): Map[String, Array[Byte]] = {
    val result: HashMap[String, Array[Byte]] = new HashMap[String, Array[Byte]]()

    // Split into apis based on path segments
    val apisMap: Map[String, Seq[RestOperation]] = operations.groupBy(o => {
      // Get number of path segments specified in config for grouping
      val pathSegements = if (o.definition.relativePathSegments.size <= config.swaggerApiGroupingPathSegment) {
        o.definition.relativePathSegments
      } else {
        o.definition.relativePathSegments.take(config.swaggerApiGroupingPathSegment)
      }

      // Only use static, non-variable, segments as the group by key
      val staticPathSegements: List[PathSegment] = pathSegements.takeWhile(ps => !ps.isVariable)
      staticPathSegements.map(ps => ps.name).mkString("/")
    })

    // Resource Listing
    val resourceListingApiPaths: Seq[String] = apisMap.keys.toSeq.sortBy(s => s)
    val resourceListingApis: Seq[ResourceListingApi] = resourceListingApiPaths.map(s => ResourceListingApi("/api-docs.json/" + s, ""))
    val resourceListing = ResourceListing(
      config.apiVersion,
      config.swaggerVersion,
      config.rootUrl,
      resourceListingApis)

    result.put(urlPath + ".json", jsonify(resourceListing))

    // API Declarations
    val apiDeclarations: Map[String, Seq[APIDeclaration]] = apisMap.map(f => {
      val (path, ops) = f

      (path, List.empty)
    })

    // Finish
    result.toMap
  }

  private def jsonify(data: AnyRef): Array[Byte] = {
    if (data == null) Array.empty else {
      implicit val formats = json.formats(NoTypeHints)
      val s = json.write(data)
      s.getBytes(CharsetUtil.UTF_8)
    }

  }
}

trait ApiDoc

/**
 * Swagger resource listing
 */
case class ResourceListing(
  apiVersion: String,
  swaggerVersion: String,
  basePath: String,
  apis: Seq[ResourceListingApi]) extends ApiDoc

case class ResourceListingApi(
  path: String,
  description: String)

/**
 * Swagger API declaration
 */
case class APIDeclaration(
  apiVersion: String,
  swaggerVersion: String,
  basePath: String,
  resourcePath: String,
  apis: Seq[ApiDef]) extends ApiDoc

object APIDeclaration {

  def apply(resourcePath: String, ops: Seq[RestOperation], config: RestConfig): APIDeclaration = {

    ops.groupBy(op => op.definition.path)
    
    APIDeclaration(
      config.apiVersion,
      config.swaggerVersion,
      config.rootUrl,
      resourcePath,
      List.empty)
  }
}

case class ApiDef(
  path: String,
  description: String,
  operations: Seq[ApiOperation])

case class ApiOperation(
  httpMethod: String,
  nickname: String,
  responseClass: String,
  parameters: Seq[ApiParameter],
  summary: String,
  notes: String)

case class ApiParameter(
  paramType: String,
  name: String,
  description: String,
  dataType: String,
  required: String //allowableValues: String,
  //allowMultiple: String
  )
  