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
  val urlPath = "/api-docs.json"

  /**
   * Generates a Map of URL paths and the associated API documentation to be returned for these paths
   *
   * @param operations Rest operations
   * @param config Rest configuration
   * @returns Map with the `key` being the exact path to match, the value is the `UTF-8` encoded response
   */
  def generate(operations: Seq[RestOperation], config: RestConfig): Map[String, Array[Byte]] = {
    val result: HashMap[String, Array[Byte]] = new HashMap[String, Array[Byte]]()

    // Group operations into resources based on path segments
    val apisMap: Map[String, Seq[RestOperation]] = operations.groupBy(o => {
      // Get number of path segments specified in config for grouping
      val pathSegements = if (o.endPoint.relativePathSegments.size <= config.swaggerApiGroupingPathSegment) {
        o.endPoint.relativePathSegments
      } else {
        o.endPoint.relativePathSegments.take(config.swaggerApiGroupingPathSegment)
      }

      // Only use static, non-variable, segments as the group by key
      val staticPathSegements: List[PathSegment] = pathSegements.takeWhile(ps => !ps.isVariable)
      staticPathSegements.map(ps => ps.name).mkString("/", "/", "")
    })

    // Resource Listing
    val resourceListing = ResourceListing(apisMap, config)
    result.put(urlPath, jsonify(resourceListing))

    // API registrations
    val apiregistrations: Map[String, APIregistration] = apisMap.map(f => {
      val (path, ops) = f
      val apiDec = APIregistration(path, ops, config)
      result.put(urlPath + path, jsonify(apiDec))
      (path, apiDec)
    })

    // Model

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

/**
 * Companion object
 */
object ResourceListing {

  /**
   * Creates a new [[org.mashupbots.socko.rest.ResourceListing]] for a group of APIs
   *
   * For example, the following operations are assumed to be in 2 resource groups: `/users` and `/pets`.
   * {{{
   * GET /users
   *
   * GET /pets
   * POST /pets/{id}
   * PUT /pets/{id}
   * }}}
   *
   * @param resources Operations grouped by their path. The grouping is specified in the key. For example,
   *   `/users` and `/pets`.
   * @param config Rest configuration
   */
  def apply(resources: Map[String, Seq[RestOperation]], config: RestConfig): ResourceListing = {
    val resourceListingApiPaths: Seq[String] = resources.keys.toSeq.sortBy(s => s)
    val resourceListingApis: Seq[ResourceListingApi] = resourceListingApiPaths.map(s => ResourceListingApi(RestApiDocGenerator.urlPath + s, ""))
    val resourceListing = ResourceListing(
      config.apiVersion,
      config.swaggerVersion,
      config.rootPath,
      resourceListingApis)

    resourceListing
  }
}

/**
 * Describes a specific resource in the resource listing
 */
case class ResourceListingApi(
  path: String,
  description: String)

/**
 * Swagger API registration
 */
case class APIregistration(
  apiVersion: String,
  swaggerVersion: String,
  basePath: String,
  resourcePath: String,
  apis: Seq[ApiPath]) extends ApiDoc

/**
 * Companion object
 */
object APIregistration {

  /**
   * Creates a new [[org.mashupbots.socko.rest.APIregistration]] for a resource path as listed in the
   * resource listing.
   *
   * For example, the following operations
   * {{{
   * POST /pets/{id}
   * PUT /pets/{id}
   * }}}
   *
   * maps to 1 ApiPath `/pets` with 2 operations: `POST` and `PUT`
   *
   * @param path Unique path. In the above example, it is `/pets/{id}`
   * @param ops HTTP method operations for that unique path
   * @param config Rest configuration
   */
  def apply(resourcePath: String, ops: Seq[RestOperation], config: RestConfig): APIregistration = {
    // Group by path so we can list the operations
    val pathGrouping: Map[String, Seq[RestOperation]] = ops.groupBy(op => op.registration.path)

    // Map group to ApiPaths
    val apiPathsMap: Map[String, ApiPath] = pathGrouping.map(f => {
      val (path, ops) = f
      (path, ApiPath(path, ops, config))
    })

    // Convert to list and sort
    val apiPaths: Seq[ApiPath] = apiPathsMap.values.toSeq.sortBy(p => p.path)

    // Build registration
    APIregistration(
      config.apiVersion,
      config.swaggerVersion,
      config.rootPath,
      resourcePath,
      apiPaths)
  }
}

/**
 * API path refers to a specific path and all the operations for that path
 */
case class ApiPath(
  path: String,
  operations: Seq[ApiOperation])

/**
 * Companion object
 */
object ApiPath {

  /**
   * Creates a new [[org.mashupbots.socko.rest.ApiPath]] for a given path
   *
   * For example, the following operations:
   * {{{
   * GET /pets/{id}
   * POST /pets/{id}
   * PUT /pets/{id}
   * }}}
   *
   * maps to 1 ApiPath `/pets` with 3 operations: `GET`, `POST` and `PUT`
   *
   * @param path Unique path
   * @param ops HTTP method operations for that unique path
   * @param config Rest configuration
   */
  def apply(path: String, ops: Seq[RestOperation], config: RestConfig): ApiPath = {
    val apiOps: Seq[ApiOperation] = ops.map(op => ApiOperation(op, config))

    ApiPath(
      path,
      apiOps.sortBy(f => f.httpMethod))
  }
}

/**
 * API operation refers to a specific HTTP operation that can be performed
 * for a path
 */
case class ApiOperation(
  httpMethod: String,
  summary: String,
  notes: String,
  deprecated: Boolean,
  responseClass: String,
  nickname: String,
  parameters: Seq[ApiParameter],
  errorResponses: Seq[ApiError])

/**
 * Companion object
 */
object ApiOperation {

  /**
   * Creates a new [[org.mashupbots.socko.rest.ApiOperation]] from a [[org.mashupbots.socko.rest.RestOperation]]
   *
   * @param op Rest operation to document
   * @param config Rest configuration
   */
  def apply(op: RestOperation, config: RestConfig): ApiOperation = {
    val params: Seq[ApiParameter] = op.deserializer.requestParamBindings.map(b => ApiParameter(b, config))
    val errors: Seq[ApiError] = op.registration.errors.map(e => ApiError(e.code, e.reason)).toSeq

    ApiOperation(
      op.registration.method.toString,
      op.registration.description,
      op.registration.notes,
      op.registration.deprecated,
      op.serializer.dataSerializer.swaggerType,
      op.registration.name,
      params,
      errors.sortBy(e => e.code))
  }
}

/**
 * API parameter refers to a path, body, query string or header parameter in
 * a [[org.mashupbots.socko.rest.ApiOperation]]
 */
case class ApiParameter(
  name: String,
  description: String,
  paramType: String,
  dataType: String,
  required: Boolean,
  allowableValues: Option[AllowableValues],
  allowMultiple: Boolean)

/**
 * Companion object
 */
object ApiParameter {

  /**
   * Creates a new [[org.mashupbots.socko.rest.ApiParameter]] for a [[org.mashupbots.socko.rest.ApiParameter]]
   *
   * @param binding parameter binding
   * @param config Configuration
   */
  def apply(binding: RequestParamBinding, config: RestConfig): ApiParameter = {
    ApiParameter(
      binding.registration.name,
      binding.registration.description,
      binding.swaggerParamType,
      binding.swaggerDataType,
      binding.required,
      binding.registration.allowableValues,
      binding.registration.allowMultiple)
  }
}

/**
 * API error refers to the HTTP response status code and its description
 */
case class ApiError(code: Int, reason: String)

