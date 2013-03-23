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
import org.mashupbots.socko.infrastructure.ReflectUtil
import org.mashupbots.socko.infrastructure.Logger
import scala.reflect.runtime.{ universe => ru }

/**
 * Collection [[org.mashupbots.socko.rest.RestOperation]]s that will be used to process
 * incoming requests.
 *
 * @param operations REST operations that will be used for processing requests
 * @param actorLookup Map of key/actor path. The key is specified in REST operation
 *   `actorPath` that are prefixed with `lookup:`. For example,
 *    {{{
 *    // Uses lookup
 *    @RestGet(uriTemplate = "/pets", actorPath = "lookup:mykey")
 *
 *    // Will NOT use lookup
 *    @RestGet(uriTemplate = "/pets", actorPath = "/my/actor/path")
 *    }}}
 *
 */
case class RestRegistry(
  operations: Seq[RestOperation],
  actorLookup: Map[String, String]) {

}

/**
 * Factory to instance a registry
 */
object RestRegistry extends Logger {

  /**
   * Instance registry using the classes under the specified package name and
   * the class loader of this class
   *
   * @param pkg Name of package where your annotated REST request and response classes
   *   are defined
   */
  def apply(pkg: String): RestRegistry = {
    apply(getClass().getClassLoader(), List(pkg), Map.empty[String, String])
  }

  /**
   * Instance a new registry using the classes under the specified package name and
   * discoverable by the specified class loader
   *
   * @param classLoader Class loader use to discover the classes in the specified package
   * @param pkg Name of package where your annotated REST request and response classes
   *   are defined
   * @param actorLookup Map of key as defined in the REST operation annotation and the
   *   corresponding actor paths
   */
  def apply(classLoader: ClassLoader, pkg: String, actorLookup: Map[String, String]): RestRegistry = {
    apply(classLoader, List(pkg), actorLookup)
  }

  /**
   * Instance a new registry using the classes under the specified package names and
   * discoverable by the specified class loader
   *
   * @param classLoader Class loader use to discover the classes in the specified package
   * @param pkg List of package names under which your annotated REST request and response
   *   classes are defined
   * @param actorLookup Map of key as defined in the REST operation annotation and the
   *   corresponding actor paths
   */
  def apply(classLoader: ClassLoader, pkg: Seq[String], actorLookup: Map[String, String]): RestRegistry = {
    val rm = ru.runtimeMirror(classLoader)
    val classes = pkg.flatMap(packageName => ReflectUtil.getClasses(classLoader, packageName))
    val classSymbols = classes.map(clz => rm.classSymbol(clz))

    val restOperations = for (
      cs <- classSymbols;
      op = findRestOperation(rm, cs);
      resp = findRestResponse(op, cs, classSymbols);
      if (op.isDefined && resp.isDefined)
    ) yield {
      log.debug("Registering {} {} {}", op.get, cs.fullName, resp.get.fullName)
      val deserializer = RestRequestDeserializer(rm, op.get, cs)
      val serializer = RestResponseSerializer(rm, op.get, resp.get)
      RestOperation(op.get, deserializer, serializer)
    }

    // Check for duplicate operation addresses
    restOperations.foreach(op => {
      val sameOp = restOperations.find(op2 => System.identityHashCode(op) != System.identityHashCode(op2) &&
        op.definition.compareAddress(op2.definition))
      if (sameOp.isDefined) {
        val msg = "Operation '%s %s' for '%s' resolves to the same address as '%s %s' for '%s'".format(
          op.definition.method, op.definition.uriTemplate, op.deserializer.requestClass.fullName,
          sameOp.get.definition.method, sameOp.get.definition.uriTemplate, sameOp.get.deserializer.requestClass.fullName)
        throw RestDefintionException(msg)
      }
    })

    RestRegistry(restOperations, actorLookup)
  }

  private val typeRestRequest = ru.typeOf[RestRequest]
  private val typeRestResponse = ru.typeOf[RestResponse]

  /**
   * Finds a REST operation annotation in a [[org.mashupbots.socko.rest.RestRequest]] class.
   *
   * @param rm Runtime mirror
   * @param cs class symbol of class to check
   * @returns An instance of the annotation class or `None` if annotation not found
   */
  def findRestOperation(rm: ru.RuntimeMirror, cs: ru.ClassSymbol): Option[RestOperationDef] = {
    val isRestRequest = cs.toType <:< typeRestRequest;
    val annotationType = RestOperationDef.findAnnotation(cs.annotations);
    if (!isRestRequest && annotationType.isEmpty) {
      None
    } else if (isRestRequest && annotationType.isEmpty) {
      log.warn("{} extends RestRequest but is not annotated with a RestOperation ", cs.fullName)
      None
    } else if (!isRestRequest && annotationType.isDefined) {
      log.warn("{} does not extend RestRequest but is annotated with a RestOperation ", cs.fullName)
      None
    } else {
      Some(RestOperationDef(annotationType.get))
    }
  }

  /**
   * Finds a corresponding response class given the operation and the request
   *
   * If operation `responseClass` field is empty, the assumed response class is the same class path
   * and name as the request class; but with `Request` suffix replaced with `Response`.
   *
   * If not empty, we will try to find the specified response class
   *
   * @param op Operation for which a response is to be located
   * @param requestClassSymbol Class Symbol for the request class
   * @param classSymbols Sequence of class symbols to check for the response class
   * @returns the response class symbol or `None` if not found
   */
  def findRestResponse(
    op: Option[RestOperationDef],
    requestClassSymbol: ru.ClassSymbol,
    classSymbols: Seq[ru.ClassSymbol]): Option[ru.ClassSymbol] = {

    val requestClassName = requestClassSymbol.fullName;

    if (op.isEmpty) {
      None
    } else if (op.get.responseClass == "") {
      // Not specified so trying finding by replacing Request in the class name
      // with Response
      val responseClassName = if (requestClassName.endsWith("Request")) {
        requestClassName.substring(0, requestClassName.length - 7) + "Response"
      } else {
        requestClassName + "Response"
      }

      val responseClassSymbol = classSymbols.find(cs => cs.fullName == responseClassName)
      if (responseClassSymbol.isEmpty) {
        log.warn("Cannot find corresponding RestResponse {} for RestRequest {}{}",
          responseClassName, requestClassName, "")
      }
      responseClassSymbol
    } else {
      // Specified so let's try to find it
      if (op.get.responseClass.contains(".")) {
        // Full path specified because we have detected a . in the name
        val responseClassSymbol = classSymbols.find(cs => cs.fullName == op.get.responseClass)
        if (responseClassSymbol.isEmpty) {
          log.warn("Cannot find corresponding RestResponse {} for RestRequest {}{}",
            op.get.responseClass, requestClassName, "")
        }
        responseClassSymbol
      } else {
        // Only class name specified
        val pkgName = requestClassSymbol.fullName.substring(0, requestClassSymbol.fullName.lastIndexOf('.'))
        val fullClassName = pkgName + "." + op.get.responseClass
        val responseClassSymbol = classSymbols.find(cs => cs.fullName == fullClassName)
        if (responseClassSymbol.isEmpty) {
          log.warn("Cannot find corresponding RestResponse {} for RestRequest {}{}",
            fullClassName, requestClassName, "")
        }
        responseClassSymbol
      }
    }
  }
}


