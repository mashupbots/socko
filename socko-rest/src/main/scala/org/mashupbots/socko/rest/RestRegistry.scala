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

import scala.reflect.runtime.{ universe => ru }

import org.mashupbots.socko.events.EndPoint
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.ReflectUtil

/**
 * Collection [[org.mashupbots.socko.rest.RestOperation]]s that will be used to process
 * incoming requests.
 *
 * @param operations REST operations that will be used for processing requests
 * @param swaggerApiDocs Swagger API documentation
 * @param config REST configuration
 */
case class RestRegistry(
  operations: Seq[RestOperation],
  swaggerApiDocs: SwaggerApiDocs,
  config: RestConfig) {

  /**
   * Finds the operation that matches the specified end point.
   *
   * If more than one operation matches, the one with the most number of static path segments
   * will be used.  If there is not a distinct operation with the most number of status path segments,
   * `None` will be returned because an exact match cannot be found.
   *
   * @param endPoint Endpoint to match
   * @return Matching [[org.mashupbots.socko.rest.RestOperation]] or `None` if not found
   */
  def findOperation(endPoint: EndPoint): Option[RestOperation] = {
    val ops = operations.filter(op => op.endPoint.matchEndPoint(endPoint))
    if (ops.size == 1) Some(ops(0))
    else if (ops.size == 0) None
    else {
      // 2 or more matches
      val sorted = ops.sortBy(op => op.endPoint.staticFullPathSegementsCount * -1) // Sort descending order
      val first = sorted(0)
      val second = sorted(1)
      if (first.endPoint.staticFullPathSegementsCount != second.endPoint.staticFullPathSegementsCount) {
        // Distinct match with the max static path segment
        Some(first)
      } else {
        // No distinct matches for the max static path segment
        None
      }
    }
  }

  /**
   * Root path that will trigger the response of swagger API documentation.  For example, `/api/api-docs.json`.
   */
  val swaggerRootApiDocsUrl = config.rootPath + SwaggerApiDocs.urlPath

  /**
   * Flag to indicate if the path requests swagger API document response.
   *
   * For example, `/api/api-docs.json` and `/api/api-docs.json/pets` will return `true` but
   * `/api/pets` will return `false`.
   *
   * @param endPoint Endpoint to check
   * @return `True` if this endpoint requires api documentation to be returned
   */
  def isSwaggerApiDocRequest(endPoint: EndPoint): Boolean = {
    endPoint.path.startsWith(swaggerRootApiDocsUrl)
  }
}

/**
 * Factory to instance a registry
 */
object RestRegistry extends Logger {

  private val typeRestRegistration = ru.typeOf[RestRegistration]
  private val typeRestRequest = ru.typeOf[RestRequest]
  private val typeRestResponse = ru.typeOf[RestResponse]
  private val typeNoSerializationRestResponse = ru.typeOf[NoSerializationRestResponse]

  /**
   * Instance registry using the classes under the specified package name and
   * the class loader of this class
   *
   * @param pkg Name of package where your annotated REST request and response classes
   *   are defined
   */
  def apply(pkg: String, config: RestConfig): RestRegistry = {
    apply(getClass().getClassLoader(), List(pkg), config)
  }

  /**
   * Instance a new registry using the classes under the specified package name and
   * discoverable by the specified class loader
   *
   * @param classLoader Class loader use to discover the classes in the specified package
   * @param pkg Name of package where your annotated REST request and response classes
   *   are defined
   */
  def apply(classLoader: ClassLoader, pkg: String, config: RestConfig): RestRegistry = {
    apply(classLoader, List(pkg), config)
  }

  /**
   * Instance a new registry using the classes under the specified package names and
   * discoverable by the specified class loader
   *
   * @param classLoader Class loader use to discover the classes in the specified package
   * @param pkg List of package names under which your annotated REST request and response
   *   classes are defined
   */
  def apply(classLoader: ClassLoader, pkg: Seq[String], config: RestConfig): RestRegistry = {
    val rm = ru.runtimeMirror(classLoader)
    val classes = pkg.flatMap(packageName => ReflectUtil.getClasses(classLoader, packageName))

    val restOperations = for (
      cs <- classes;
      op = buildRestOperation(rm, cs, classes, config);
      if (op.isDefined)
    ) yield {
      op.get
    }

    // Check for duplicate operation paths
    restOperations.foreach(op => {
      val sameOp = restOperations.find(op2 => System.identityHashCode(op) != System.identityHashCode(op2) &&
        op.endPoint.comparePath(op2.endPoint))
      if (sameOp.isDefined) {
        val msg = "Operation '%s %s' for '%s' resolves to the same address as '%s %s' for '%s'".format(
          op.endPoint.method, op.endPoint.relativePath, op.deserializer.requestClass.fullName,
          sameOp.get.endPoint.method, sameOp.get.endPoint.relativePath, sameOp.get.deserializer.requestClass.fullName)
        throw RestDefintionException(msg)
      }
    })

    val swaggerApiDoc = SwaggerApiDocs(restOperations, config, rm)
    
    RestRegistry(restOperations, swaggerApiDoc, config)
  }

  /**
   * Builds a [[org.mashupbots.socko.rest.RestOperation]] for a specific class `clz`.
   *
   * If `clz` is a child of [[org.mashupbots.socko.rest.RestRegistration]], and it has a corresponding
   * [[org.mashupbots.socko.rest.RestRequest]] and [[org.mashupbots.socko.rest.RestResponse]], then
   * a [[org.mashupbots.socko.rest.RestOperation]] is instanced and returned.
   *
   * If not, then `None` is returned.
   *
   * @param rm Runtime mirror
   * @param clz class to check
   * @param classes Collection of classes found in the package to load
   * @param config REST configuration
   * @return An instance of the annotation class or `None` if annotation not found
   */
  def buildRestOperation(rm: ru.Mirror, clz: Class[_], classes: Seq[Class[_]], config: RestConfig): Option[RestOperation] = {
    val registration = findRestRegistration(rm, clz, config);
    val req = findRestRequest(registration, rm, clz, classes)
    val resp = findRestResponse(registration, rm, clz, classes);

    if (registration.isDefined && req.isDefined && resp.isDefined) {
      val endPoint = RestEndPoint(config, registration.get) 
      val deserializer = RestRequestDeserializer(config, rm, registration.get, endPoint, req.get.typeSymbol.asClass)
      val serializer = RestResponseSerializer(config, rm, registration.get, resp.get.typeSymbol.asClass)
      log.info("Registering {} {} {}", endPoint.method, endPoint.fullPath, clz.getName)

      Some(RestOperation(registration.get, endPoint, deserializer, serializer))
    } else {
      None
    }
  }

  /**
   * Finds a REST operation annotation in a [[org.mashupbots.socko.rest.RestRequest]] class.
   *
   * @param rm Runtime mirror
   * @param clz class to check
   * @param config REST configuration
   * @return An instance of the annotation class or `None` if annotation not found
   */
  def findRestRegistration(rm: ru.RuntimeMirror, clz: Class[_], config: RestConfig): Option[RestRegistration] = {
    val moduleSymbol = rm.moduleSymbol(clz)
    val moduleType = moduleSymbol.typeSignature
    if (moduleType <:< typeRestRegistration) {
      val moduleMirror = rm.reflectModule(moduleSymbol)
      val obj = moduleMirror.instance
      Some(obj.asInstanceOf[RestRegistration])
    } else {
      None
    }
  }

  /**
   * Finds a corresponding request class given the registration
   *
   * If registration `requestClass` field is empty, the assumed request class is the same class path
   * and name as the registration class; but with `registration` suffix replaced with `Request`.
   *
   * If not empty, the specified request type will be used
   *
   * @param registration REST operation registration details
   * @param rm Mirror
   * @param clz registration class
   * @param classes Sequence of classes in which to search for the request class
   * @return the request type or `None` if not found
   */
  def findRestRequest(
    registration: Option[RestRegistration],
    rm: ru.RuntimeMirror,
    clz: Class[_],
    classes: Seq[Class[_]]): Option[ru.Type] = {

    if (registration.isEmpty) {
      None
    } else {
      if (registration.get.request.isEmpty) {
        val requestClassName = replaceRegistrationInName(clz.getName, "Request")
        val requestClass = classes.find(c => c.getName == requestClassName && rm.classSymbol(c).toType <:< typeRestRequest)
        if (requestClass.isEmpty) {
          throw RestDefintionException(s"Cannot find corresponding RestRequest '${requestClassName}' for RestRegistration '${clz.getName}'")
        }
        Some(rm.classSymbol(requestClass.get).toType)
      } else {
        Some(registration.get.request.get)
      }
    }
  }

  /**
   * Finds a corresponding response class given the registration
   *
   * If registration `responseClass` field is empty, the assumed response class is the same class path
   * and name as the registration class; but with `registration` suffix replaced with `Response`.
   *
   * If registration `responseClass` field is not empty, the specified response type will be used
   *
   * If `customSerialization` is declared, the standard `NoSerializationRestResponse` will be returned.
   * This is just a placeholder and will not be used because the processing actor will handle serialization.
   *
   * @param registration REST operation registration details
   * @param rm Mirror
   * @param clz registration class
   * @param classes Sequence of classes in which to search for the request class
   * @return the response type or `None` if not found
   */
  def findRestResponse(
    registration: Option[RestRegistration],
    rm: ru.RuntimeMirror,
    clz: Class[_],
    classes: Seq[Class[_]]): Option[ru.Type] = {

    if (registration.isEmpty) {
      None
    } else {
      if (registration.get.customSerialization) {
        Some(typeNoSerializationRestResponse)
      } else if (registration.get.response.isEmpty) {
        val responseClassName = replaceRegistrationInName(clz.getName, "Response")
        val responseClass = classes.find(c => c.getName == responseClassName && rm.classSymbol(c).toType <:< typeRestResponse)
        if (responseClass.isEmpty) {
          throw RestDefintionException(s"Cannot find corresponding RestResponse '${responseClassName}' for RestRegistration '${clz.getName}'")
        }
        Some(rm.classSymbol(responseClass.get).toType)
      } else {
        Some(registration.get.response.get)
      }
    }
  }

  private def replaceRegistrationInName(requestClassName: String, newSuffix: String): String = {
    if (requestClassName.endsWith("Registration")) {
      requestClassName.substring(0, requestClassName.length - 12) + newSuffix
    } else {
      requestClassName + newSuffix
    }
  }

}

/**
 * Class to denote that no serialization is required because it will be custom handled
 * by the REST processing actor
 */
case class NoSerializationRestResponse(context: RestResponseContext) extends RestResponse
