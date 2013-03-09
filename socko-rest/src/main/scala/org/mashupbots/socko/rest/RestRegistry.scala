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
import scala.reflect.runtime.{ universe => ru }
import org.mashupbots.socko.infrastructure.Logger

/**
 * Collection of meta data about REST endpoints
 *
 * @param operations REST operations that will be used for dispatching requests
 * @param actorLookup Map of key as defined in the RestEndPoint annotation and the
 *   corresponding actor paths
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
   * @param actorLookup Map of key as defined in the RestEndPoint annotation and the
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
   * @param actorLookup Map of key as defined in the RestEndPoint annotation and the
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
      log.debug("Registering ")
      RestOperation(op.get, cs, cs)
    }

    RestRegistry(restOperations, actorLookup)
  }

  private val typeRestRequest = ru.typeOf[RestRequest]
  private val typeRestResponse = ru.typeOf[RestResponse]
  private val typeRestOperation = ru.typeOf[RestDeclaration]

  /**
   * Finds a [[org.mashupbots.socko.rest.RestOperation]] annotation in a
   * [[org.mashupbots.socko.rest.RestRequest]].
   *
   * @param rm Runtime mirror
   * @param cs class symbol of class to check
   * @returns An instance of the annotation class or `None` if annotation not found
   */
  def findRestOperation(rm: ru.RuntimeMirror, cs: ru.ClassSymbol): Option[RestDeclaration] = {
    val isRestRequest = cs.toType <:< typeRestRequest;
    val annotationType = cs.annotations.find(a => a.tpe <:< typeRestOperation);
    if (!isRestRequest && annotationType.isEmpty) {
      None
    } else if (isRestRequest && annotationType.isEmpty) {
      log.warn("{} extends RestRequest but is not annotated with a RestOperation ", cs.fullName)
      None
    } else if (!isRestRequest && annotationType.isDefined) {
      log.warn("{} does not extend RestRequest but is annotated with a RestOperation ", cs.fullName)
      None
    } else {
      val a = annotationType.get
      val aa = a.scalaArgs
      val bb = a.scalaArgs(0)
      val xx = a.scalaArgs(2)
      val args = a.scalaArgs.map(a => a.productElement(0).asInstanceOf[ru.Constant].value)

      val classMirror = rm.reflectClass(a.tpe.typeSymbol.asClass)
      val constructorMethodSymbol = a.tpe.declaration(ru.nme.CONSTRUCTOR).asMethod
      val constructorMethodMirror = classMirror.reflectConstructor(constructorMethodSymbol)

      val restOperation = constructorMethodMirror(args: _*).asInstanceOf[RestDeclaration]
      Some(restOperation)
    }
  }

  /**
   * Finds a corresponding response class given the operation and the request
   *
   * If operation `responseClass` is empty, the assumed response class is the same class path
   * and name as the request class; but with `Request` suffix replaced with `Response`.
   *
   * If not empty, we will try to find the specified response class
   *
   * @param op RestOperation
   * @param requestClassSymbol Class Symbol for the request class
   * @param classSymbols Sequence of class symbols to check for the response class
   * @returns the response class symbol or `None` if not found
   */
  def findRestResponse(
    op: Option[RestDeclaration],
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

      val responseClassSymbol = classSymbols.find(cs => cs.fullName == requestClassName)
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
        val fullClassName = pkgName + op.get.responseClass
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


