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
 * @param config REST configuration
 */
case class RestRegistry(
  operations: Seq[RestOperation],
  config: RestConfig) {

  /**
   * Finds the operation that matches the specified end point
   *
   * @param endPoint Endpoint to match
   * @returns Matching [[org.mashupbots.socko.rest.RestOperation]]
   */
  def findOperation(endPoint: EndPoint): RestOperation = {
    val op = operations.find(op => op.definition.matchEndPoint(endPoint))
    if (op.isEmpty) {
      throw RestBindingException(s"Cannot find operation for request to: '${endPoint.method} ${endPoint.path}'")
    }
    op.get
  }

}

/**
 * Factory to instance a registry
 */
object RestRegistry extends Logger {

  private val typeRestRequest = ru.typeOf[RestRequest]
  private val typeRestResponse = ru.typeOf[RestResponse]
  private val typeRestProcessorLocator = ru.typeOf[RestDispatcher]
  private val dispatcherClassCache = scala.collection.mutable.Map.empty[String, RestDispatcher]
  private val csNoSerializationRestResponse = ru.typeOf[NoSerializationRestResponse].typeSymbol.asClass

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
    val classSymbols = classes.map(clz => rm.classSymbol(clz))

    val restOperations = for (
      cs <- classSymbols;
      op = findRestOperation(rm, cs, config);
      resp = findRestResponse(op, cs, classSymbols);
      dispatcher = findRestDispatcher(rm, op, cs, classSymbols);
      if (op.isDefined && resp.isDefined && dispatcher.isDefined)
    ) yield {
      log.debug("Registering {} {} {}", op.get, cs.fullName, resp.get.fullName)
      val deserializer = RestRequestDeserializer(config, rm, op.get, cs)
      val serializer = RestResponseSerializer(config, rm, op.get, resp.get)
      RestOperation(op.get, dispatcher.get, deserializer, serializer)
    }

    // Check for duplicate operation addresses
    restOperations.foreach(op => {
      val sameOp = restOperations.find(op2 => System.identityHashCode(op) != System.identityHashCode(op2) &&
        op.definition.compareUrlTemplate(op2.definition))
      if (sameOp.isDefined) {
        val msg = "Operation '%s %s' for '%s' resolves to the same address as '%s %s' for '%s'".format(
          op.definition.method, op.definition.urlTemplate, op.deserializer.requestClass.fullName,
          sameOp.get.definition.method, sameOp.get.definition.urlTemplate, sameOp.get.deserializer.requestClass.fullName)
        throw RestDefintionException(msg)
      }
    })

    RestRegistry(restOperations, config)
  }

  /**
   * Finds a REST operation annotation in a [[org.mashupbots.socko.rest.RestRequest]] class.
   *
   * @param rm Runtime mirror
   * @param cs class symbol of class to check
   * @param config REST configuration
   * @returns An instance of the annotation class or `None` if annotation not found
   */
  def findRestOperation(rm: ru.RuntimeMirror, cs: ru.ClassSymbol, config: RestConfig): Option[RestOperationDef] = {
    val isRestRequest = cs.toType <:< typeRestRequest;
    val annotationType = RestOperationDef.findAnnotation(cs);
    if (!isRestRequest && annotationType.isEmpty) {
      None
    } else if (isRestRequest && annotationType.isEmpty) {
      log.warn("{} extends RestRequest but is not annotated with a Rest operation", cs.fullName)
      None
    } else if (!isRestRequest && annotationType.isDefined) {
      log.warn("{} does not extend RestRequest but is annotated with a Rest operation", cs.fullName)
      None
    } else {
      Some(RestOperationDef(annotationType.get, config))
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
   * If customSerialization is turned on, the standard `NoSerializationRestResponse` will be returned.
   * This is just a placeholder and will not be used because the processing actor will handle serialization.
   *
   * @param opDef Definition of the operation for which a response is to be located
   * @param requestClassSymbol Class Symbol for the request class
   * @param classSymbols Sequence of class symbols in which to search for the response class
   * @returns the response class symbol or `None` if not found
   */
  def findRestResponse(
    opDef: Option[RestOperationDef],
    requestClassSymbol: ru.ClassSymbol,
    classSymbols: Seq[ru.ClassSymbol]): Option[ru.ClassSymbol] = {

    val requestClassName = requestClassSymbol.fullName

    def findClassSymbol(fullClassName: String) = classSymbols.find(cs => cs.fullName == fullClassName &&
      cs.toType <:< typeRestResponse)

    if (opDef.isEmpty) {
      None
    } else {
      if (opDef.get.customSerialization) {
        // Return a standard response that will not be used because the processor actor will handle it
        Some(csNoSerializationRestResponse)
      } else {
        val responseClassName =
          if (opDef.get.responseClass == "") replaceRequestInName(requestClassName, "Response")
          else if (opDef.get.responseClass.contains(".")) opDef.get.responseClass
          else {
            val pkgName = requestClassSymbol.fullName.substring(0, requestClassSymbol.fullName.lastIndexOf('.'))
            pkgName + "." + opDef.get.responseClass
          }

        val responseClassSymbol = findClassSymbol(responseClassName)
        if (responseClassSymbol.isEmpty) {
          log.warn("Cannot find corresponding RestResponse '{}' for RestRequest '{}'{}",
            responseClassName, requestClassName, "")
        }
        responseClassSymbol
      }
    }
  }

  /**
   * Finds and instances a dispatcher class given the operation and the request
   *
   * If operation `dispatcherClass` field is empty, the class is assumed the same class path
   * and name as the request class; but with `Request` suffix replaced with `Dispatcher`.
   *
   * If not empty, we will try to find the specified dispatcher class
   *
   * @param rm Runtime mirror
   * @param opDef Definition of the operation for which a response is to be located
   * @param requestClassSymbol Class Symbol for the request class
   * @param classSymbols Sequence of class symbols in which to search for the processor locator class
   * @returns the response class symbol or `None` if not found
   */
  def findRestDispatcher(
    rm: ru.RuntimeMirror,
    opDef: Option[RestOperationDef],
    requestClassSymbol: ru.ClassSymbol,
    classSymbols: Seq[ru.ClassSymbol]): Option[RestDispatcher] = {

    val requestClassName = requestClassSymbol.fullName

    def findClassSymbol(fullClassName: String) = classSymbols.find(cs =>
      cs.fullName == fullClassName && cs.toType <:< typeRestProcessorLocator)

    // Find
    val dispatcherClassSymbol = if (opDef.isEmpty) {
      None
    } else {
      val dispatcherClassName =
        if (opDef.get.dispatcherClass == "") replaceRequestInName(requestClassName, "Dispatcher")
        else if (opDef.get.dispatcherClass.contains(".")) opDef.get.dispatcherClass
        else {
          val pkgName = requestClassSymbol.fullName.substring(0, requestClassSymbol.fullName.lastIndexOf('.'))
          pkgName + "." + opDef.get.dispatcherClass
        }
      val dispatcherClassSymbol = findClassSymbol(dispatcherClassName)
      if (dispatcherClassSymbol.isEmpty) {
        log.warn("Cannot find corresponding RestResponse '{}' for RestRequest '{}'{}",
          dispatcherClassName, requestClassName, "")
      }
      dispatcherClassSymbol
    }

    // Instance
    if (dispatcherClassSymbol.isEmpty) {
      None
    } else {
      val cs = dispatcherClassSymbol.get
      val cachedObj = dispatcherClassCache.get(cs.fullName)
      if (cachedObj.isDefined) cachedObj
      else {
        val classMirror = rm.reflectClass(cs)
        val constructorMethodSymbol = cs.toType.declaration(ru.nme.CONSTRUCTOR).asMethod
        val constructorMethodMirror = classMirror.reflectConstructor(constructorMethodSymbol)
        val x = constructorMethodSymbol.paramss
        if (constructorMethodSymbol.paramss(0).size != 0) {
          log.warn("RestDispatcher '{}' for RestRequest '{}' does not have a parameterless constructor{}",
            cs.fullName, requestClassName, "")
          None
        } else {
          val obj = constructorMethodMirror().asInstanceOf[RestDispatcher]
          dispatcherClassCache.put(cs.fullName, obj)
          Some(obj)
        }
      }
    }
  }

  private def replaceRequestInName(requestClassName: String, newSuffix: String): String = {
    if (requestClassName.endsWith("Request")) {
      requestClassName.substring(0, requestClassName.length - 7) + newSuffix
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
