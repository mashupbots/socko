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

import scala.collection.mutable.HashMap
import scala.reflect.runtime.{universe => ru}
import org.json4s.NoTypeHints
import org.json4s.native.{Serialization => json}
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.events.EndPoint

/**
 * Generated [[https://developers.helloreverb.com/swagger/ Swagger]] API documentation
 * 
 * @param lookup Map of path and swagger JSON associated with the path
 */
case class SwaggerApiDocs(lookup: Map[String, Array[Byte]]) {
  
  /**
   * Gets the JSON doc for the specified end point
   * 
   * @param path Full path to the requested documentation. For example: `/api/api-docs.json/pet`.
   * @return JSON UTF-8, `None` if data for path not found
   */
  def get(path: String): Option[Array[Byte]] = {
    lookup.get(path)
  }
}

/**
 * Companion object
 */
object SwaggerApiDocs extends Logger {

  /**
   * URL path relative to the config `rootUrl` that will trigger the return of the API documentation
   */
  val urlPath = "/api-docs.json"
    
  /**
   * Generates a Map of URL paths and the associated API documentation to be returned for these paths
   *
   * @param operations Rest operations
   * @param config Rest configuration
   * @param rm Runtime Mirror used to reflect property meta data
   * @return Map with the `key` being the exact path to match, the value is the JSON string
   */
  def apply(operations: Seq[RestOperation], config: RestConfig, rm: ru.Mirror): SwaggerApiDocs = {
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
    val resourceListing = SwaggerResourceListing(apisMap, config)
    result.put(config.rootPath + urlPath, jsonify(resourceListing))

    // API registrations
    val apiregistrations: Map[String, SwaggerApiDeclaration] = apisMap.map(f => {
      val (path, ops) = f
      val apiDec = SwaggerApiDeclaration(path, ops, config, rm)
      result.put(config.rootPath + urlPath + path, jsonify(apiDec))
      (path, apiDec)
    })

    // Finish
    SwaggerApiDocs(result.toMap)
  }

  private def jsonify(data: AnyRef): Array[Byte] = {
    if (data == null) Array.empty else {
      implicit val formats = json.formats(NoTypeHints)
      val s = json.write(data)
      s.getBytes(CharsetUtil.UTF_8)
    }

  }
}

trait SwaggerDoc

/**
 * Swagger [[https://github.com/wordnik/swagger-core/wiki/Resource-Listing resource listing]]
 */
case class SwaggerResourceListing(
  apiVersion: String,
  swaggerVersion: String,
  basePath: String,
  apis: Seq[SwaggerResourceListingApi]) extends SwaggerDoc

/**
 * Companion object
 */
object SwaggerResourceListing {

  /**
   * Creates a new [[org.mashupbots.socko.rest.SwaggerResourceListing]] for a group of APIs
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
   * @param config REST configuration
   */
  def apply(resources: Map[String, Seq[RestOperation]], config: RestConfig): SwaggerResourceListing = {
    val resourceListingSwaggerApiPaths: Seq[String] = resources.keys.toSeq.sortBy(s => s)
    val resourceListingApis: Seq[SwaggerResourceListingApi] =
      resourceListingSwaggerApiPaths.map(s => SwaggerResourceListingApi(SwaggerApiDocs.urlPath + s, ""))
    val resourceListing = SwaggerResourceListing(
      config.apiVersion,
      config.swaggerVersion,
      config.rootApiUrl,
      resourceListingApis)

    resourceListing
  }
}

/**
 * Describes a specific resource in the resource listing
 */
case class SwaggerResourceListingApi(
  path: String,
  description: String)

/**
 * Swagger [[https://github.com/wordnik/swagger-core/wiki/API-Declaration API declaration]]
 */
case class SwaggerApiDeclaration(
  apiVersion: String,
  swaggerVersion: String,
  basePath: String,
  resourcePath: String,
  apis: Seq[SwaggerApiPath],
  models: Map[String, SwaggerModel]) extends SwaggerDoc

/**
 * Companion object
 */
object SwaggerApiDeclaration {

  /**
   * Creates a new [[org.mashupbots.socko.rest.SwaggerApiDeclaration]] for a resource path as listed in the
   * resource listing.
   *
   * For example, the following operations
   * {{{
   * POST /pets/{id}
   * PUT /pets/{id}
   * }}}
   *
   * maps to 1 SwaggerApiPath `/pets` with 2 operations: `POST` and `PUT`
   *
   * @param resourcePath Unique path. In the above example, it is `/pets/{id}`
   * @param ops HTTP method operations for that unique path
   * @param config REST configuration
   * @param rm Runtime mirror for reflecting meta data
   */
  def apply(resourcePath: String, ops: Seq[RestOperation], config: RestConfig, rm: ru.Mirror): SwaggerApiDeclaration = {
    // Context for this resource path
    val ctx = SwaggerContext(config, SwaggerModelRegistry(rm))

    // Group by path so we can list the operations
    val pathGrouping: Map[String, Seq[RestOperation]] = ops.groupBy(op => op.registration.path)

    // Map group to SwaggerApiPaths
    val apiPathsMap: Map[String, SwaggerApiPath] = pathGrouping.map(f => {
      val (path, ops) = f
      (path, SwaggerApiPath(path, ops, ctx))
    })

    // Convert to list and sort
    val apiPaths: Seq[SwaggerApiPath] = apiPathsMap.values.toSeq.sortBy(p => p.path)

    // Build registration
    SwaggerApiDeclaration(
      config.apiVersion,
      config.swaggerVersion,
      config.rootApiUrl,
      resourcePath,
      apiPaths,
      ctx.modelRegistry.models.toMap)
  }
}

/**
 * API path refers to a specific path and all the operations for that path
 */
case class SwaggerApiPath(
  path: String,
  operations: Seq[SwaggerApiOperation])

/**
 * Companion object
 */
object SwaggerApiPath {

  /**
   * Creates a new [[org.mashupbots.socko.rest.SwaggerApiPath]] for a given path
   *
   * For example, the following operations:
   * {{{
   * GET /pets/{id}
   * POST /pets/{id}
   * PUT /pets/{id}
   * }}}
   *
   * maps to 1 SwaggerApiPath `/pets` with 3 operations: `GET`, `POST` and `PUT`
   *
   * @param path Unique path
   * @param ops HTTP method operations for that unique path
   * @param ctx Processing context
   */
  def apply(path: String, ops: Seq[RestOperation], ctx: SwaggerContext): SwaggerApiPath = {
    val apiOps: Seq[SwaggerApiOperation] = ops.map(op => SwaggerApiOperation(op, ctx))
    SwaggerApiPath(
      path,
      apiOps.sortBy(f => f.httpMethod))
  }
}

/**
 * API operation refers to a specific HTTP operation that can be performed
 * for a path
 */
case class SwaggerApiOperation(
  httpMethod: String,
  summary: Option[String],
  notes: Option[String],
  deprecated: Option[Boolean],
  responseClass: String,
  nickname: String,
  parameters: Option[Seq[SwaggerApiParameter]],
  errorResponses: Option[Seq[SwaggerApiError]])

/**
 * Companion object
 */
object SwaggerApiOperation {

  /**
   * Creates a new [[org.mashupbots.socko.rest.SwaggerApiOperation]] from a [[org.mashupbots.socko.rest.RestOperation]]
   *
   * @param op Rest operation to document
   * @param ctx Processing context
   */
  def apply(op: RestOperation, ctx: SwaggerContext): SwaggerApiOperation = {
    val params: Seq[SwaggerApiParameter] = op.deserializer.requestParamBindings.map(b => SwaggerApiParameter(b, ctx))
    val errors: Seq[SwaggerApiError] = op.registration.errors.map(e => SwaggerApiError(e.code, e.reason)).toSeq

    val swaggerType: String = op.serializer.dataSerializer match {
      case s: VoidDataSerializer =>
        "void"
      case s: ObjectDataSerializer =>
        ctx.modelRegistry.register(s.tpe)
        SwaggerReflector.dataType(s.tpe)
      case s: PrimitiveDataSerializer =>
        SwaggerReflector.dataType(s.tpe)
      case s: ByteArrayDataSerializer =>
        SwaggerReflector.dataType(ru.typeOf[Array[Byte]])
      case s: ByteSeqDataSerializer =>
        SwaggerReflector.dataType(ru.typeOf[Seq[Byte]])
      case _ =>
        throw new IllegalStateException("Unsupported DataSerializer: " + op.serializer.dataSerializer.toString)
    }

    SwaggerApiOperation(
      op.registration.method.toString,
      if (op.registration.description.isEmpty) None else Some(op.registration.description),
      if (op.registration.notes.isEmpty) None else Some(op.registration.notes),
      if (op.registration.deprecated) Some(true) else None,
      swaggerType,
      op.registration.name,
      if (params.isEmpty) None else Some(params),
      if (errors.isEmpty) None else Some(errors.sortBy(e => e.code)))
  }
}

/**
 * API [[https://github.com/wordnik/swagger-core/wiki/Parameters parameter]] refers to a path, body, query string or
 * header parameter in a [[org.mashupbots.socko.rest.SwaggerApiOperation]]
 */
case class SwaggerApiParameter(
  name: String,
  description: Option[String],
  paramType: String,
  dataType: String,
  required: Option[Boolean],
  allowableValues: Option[AllowableValues],
  allowMultiple: Option[Boolean])

/**
 * Companion object
 */
object SwaggerApiParameter {

  /**
   * Creates a new [[org.mashupbots.socko.rest.SwaggerApiParameter]] for a [[org.mashupbots.socko.rest.SwaggerApiParameter]]
   *
   * @param binding parameter binding
   * @param ctx Processing context
   */
  def apply(binding: RequestParamBinding, ctx: SwaggerContext): SwaggerApiParameter = {
    val swaggerParamType: String = binding match {
      case _: PathBinding => "path"
      case _: QueryStringBinding => "query"
      case _: HeaderBinding => "header"
      case _: BodyBinding =>
        ctx.modelRegistry.register(binding.tpe)
        "body"
      case _ => throw new IllegalStateException("Unsupported RequestParamBinding: " + binding.toString)
    }

    val swaggerDataType: String = SwaggerReflector.dataType(binding.tpe)

    SwaggerApiParameter(
      binding.registration.name,
      if (binding.registration.description.isEmpty()) None else Some(binding.registration.description),
      swaggerParamType,
      swaggerDataType,
      if (binding.required) Some(true) else None,
      binding.registration.allowableValues,
      if (binding.registration.allowMultiple) Some(true) else None)
  }
}

/**
 * API error refers to the HTTP response status code and its description
 */
case class SwaggerApiError(code: Int, reason: String)

/**
 * A swagger model complex data type's properties
 *
 * @param type Swagger data type
 * @param description Description of the property
 * @param required Boolean to indicate if the property is required. If `None`, `false` is assumed.
 * @param allowableValues Optional allowable list or range of values
 * @param items Only applicable for containers. Defines the data type of items in a container.
 *   For primitives, it is `"type":"string"`.  For complex types, it is `"ref":"Category"`.
 */
case class SwaggerModelProperty(
  `type`: String,
  description: Option[String],
  required: Option[Boolean],
  allowableValues: Option[AllowableValues],
  items: Option[Map[String, String]])

/**
 * A swagger model complex data type
 *
 * @param id Unique id
 * @param description description
 * @param properties List of properties
 */
case class SwaggerModel(
  id: String,
  description: Option[String],
  properties: Map[String, SwaggerModelProperty])

/**
 * Registry of swagger models. Makes sure that we don't output a model more than once.
 *
 * @param rm Runtime Mirror
 */
case class SwaggerModelRegistry(rm: ru.Mirror) {
  val models: HashMap[String, SwaggerModel] = new HashMap[String, SwaggerModel]()

  val typeRestModelMetaData = ru.typeOf[RestModelMetaData]

  /**
   * Registers a complex type in the swagger model
   */
  private def registerComplexType(tpe: ru.Type) = {
    // If this is an option, get the base type
    val thisType = SwaggerReflector.optionContentType(tpe)

    // Add to model if not already added
    val name = SwaggerReflector.dataType(thisType)
    if (!models.contains(name)) {
      // Sub complex types to that may also need reflecting
      val subModels = collection.mutable.Set[ru.Type]()

      // Get properties meta data
      val propertiesMetaData = locatePropertiesMetaData(thisType)

      // Get properties of this model
      val properties: Map[String, SwaggerModelProperty] =
        thisType.members
          .filter(s => s.isTerm && !s.isMethod && !s.isMacro)
          .map(s => {
            val dot = s.fullName.lastIndexOf('.')
            val termName = if (dot > 0) s.fullName.substring(dot + 1) else s.fullName
            val required = !(s.typeSignature <:< SwaggerReflector.optionAnyRefType)
            val termRequired = if (required) Some(true) else None
            val (termType: String, items: Map[String, String]) = parsePropertyType(s.typeSignature, subModels)
            val termItems = if (items.isEmpty) None else Some(items)

            val metaData: Option[RestPropertyMetaData] = propertiesMetaData.find(p => p.name == termName)
            val description: Option[String] =
              if (metaData.isEmpty) None
              else if (metaData.get.description.isEmpty) None
              else Some(metaData.get.description)
            val allowableValues: Option[AllowableValues] = if (metaData.isEmpty) None else metaData.get.allowableValues

            // Return name-value for map
            (termName, SwaggerModelProperty(termType, description, termRequired, allowableValues, termItems))
          }).toMap

      // Add model to registry
      val model = SwaggerModel(name, None, properties)
      models.put(name, model)

      // Add sub-models - we do this after we add to model to cyclical entries
      subModels.foreach(sm => register(sm))
    }
  }

  /**
   * Parse the type of a property and output the Swagger API details
   *
   * @param tpe Type to parse
   * @param subModels Complex Types and Complex Types within a container are added to this set so that
   *   they can be registered as a model object for swagger output.
   * @return Tuple of swagger data type name and map of swagger container content type. Non container classes
   *   returns an empty map of container content type.
   */
  private def parsePropertyType(tpe: ru.Type, subModels: collection.mutable.Set[ru.Type]): (String, Map[String, String]) = {
    if (SwaggerReflector.isPrimitive(tpe)) {
      // Primitive
      (SwaggerReflector.dataType(tpe), Map.empty[String, String])
    } else {
      val containerType = SwaggerReflector.containerType(tpe)
      if (containerType == "") {
        // Complex
        subModels.add(tpe)
        (SwaggerReflector.dataType(tpe), Map.empty[String, String])
      } else {
        // Container
        val contentType = SwaggerReflector.containerContentType(tpe)
        if (SwaggerReflector.isPrimitive(contentType)) {
          // Container of primitives
          (containerType, Map[String, String]("type" -> SwaggerReflector.dataType(contentType)))
        } else {
          // Container of complex types
          subModels.add(contentType)
          (containerType, Map[String, String]("$ref" -> SwaggerReflector.dataType(contentType)))
        }
      }
    }
  }

  /**
   * Finds the companion object of `tpe` and if it extends [[org.mashupbots.socko.rest.RestModelMetaData]],
   * `modelProperties` is returned.
   *
   * @param tpe Type to investigate
   * @return Sequence of [[org.mashupbots.socko.rest.RestModelMetaData]]. Empty if no extra meta data found.
   */
  private def locatePropertiesMetaData(tpe: ru.Type): Seq[RestPropertyMetaData] = {
    val cs = tpe.typeSymbol.asClass
    val companionModuleSymbol = cs.companionSymbol.asModule
    val moduleType = companionModuleSymbol.typeSignature
    if (moduleType <:< typeRestModelMetaData) {
      val moduleMirror = rm.reflectModule(companionModuleSymbol)
      val companionObj = moduleMirror.instance.asInstanceOf[RestModelMetaData]
      companionObj.modelProperties
    } else {
      Seq.empty
    }
  }

  /**
   * Determines if we need to register a type as a model. Primitives are ignored.
   *
   * @param tpe Type to register
   */
  def register(tpe: ru.Type): Unit = {
    if (SwaggerReflector.isPrimitive(tpe))
      // Ignore primitive
      Unit
    else {
      val containerType = SwaggerReflector.containerType(tpe)
      if (containerType == "") {
        // Register complex type
        registerComplexType(tpe)
      } else {
        // Container type
        val contentType = SwaggerReflector.containerContentType(tpe)
        if (SwaggerReflector.isPrimitive(contentType))
          // Ignore primitive containers
          Unit
        else
          // Register Container of complex types
          registerComplexType(contentType)
      }
    }
  }
}

case class SwaggerContext(config: RestConfig, modelRegistry: SwaggerModelRegistry)

    
                      