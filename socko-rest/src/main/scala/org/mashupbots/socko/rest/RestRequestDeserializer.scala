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

import scala.reflect.runtime.{ universe => ru }
import org.mashupbots.socko.infrastructure.ReflectUtil
import java.util.Date
import java.text.SimpleDateFormat
import org.mashupbots.socko.infrastructure.DateUtil

/**
 * Deserializes incoming data into a [[org.mashupbots.socko.rest.RestRequest]]
 *
 * @param requestConstructor Constructor to call when instancing the request class
 * @param params Bindings to extract values form the request data. The values will be
 *   passed into the requestConstructor to instance the request class.
 */
case class RestRequestDeserializer(
  requestClass: ru.ClassSymbol,
  requestConstructorMirror: ru.MethodMirror,
  requestParamBindings: List[RequestParamBinding]) {

  def deserialize(context: RestRequestContext): RestRequest = {

    val params: List[_] = context :: requestParamBindings.map(b => b.extract(context))
    requestConstructorMirror(params: _*).asInstanceOf[RestRequest]
  }
}

/**
 * Companion object
 */
object RestRequestDeserializer {
  private val restRequestContextType = ru.typeOf[RestRequestContext]
  private val restResponseContextType = ru.typeOf[RestResponseContext]

  /**
   * Factory for RestRequestBinding
   *
   * @param rm Runtime Mirror with the same class loaders as the specified request class
   * @param definition Definition of the operation
   * @param requestClassSymbol Request class symbol
   */
  def apply(rm: ru.Mirror, definition: RestOperationDef, requestClassSymbol: ru.ClassSymbol): RestRequestDeserializer = {
    val requestConstructor: ru.MethodSymbol = requestClassSymbol.toType.declaration(ru.nme.CONSTRUCTOR).asMethod
    val requestConstructorMirror: ru.MethodMirror = rm.reflectClass(requestClassSymbol).reflectConstructor(requestConstructor)

    val requestConstructorParams: List[ru.TermSymbol] = requestConstructor.paramss(0).map(p => p.asTerm)

    // First param better be  RestRequestContext
    if (requestConstructorParams.head.typeSignature != restRequestContextType) {
      throw RestDefintionException(s"First constructor parameter of '${requestClassSymbol.fullName}' must be of type RestRequestContext.")
    }

    val params = requestConstructorParams.tail.map(p => RequestParamBinding(definition, requestClassSymbol, p))

    RestRequestDeserializer(requestClassSymbol, requestConstructorMirror, params)
  }

}

/**
 * Binding of a request value
 */
trait RequestParamBinding {

  /**
   * Name of the binding
   */
  def name: String

  /**
   * Description
   */
  def description: String

  /**
   * Type of the parameter binding
   */
  def tpe: ru.Type

  /**
   * Flag to denote if this parameter is required
   */
  def required: Boolean

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext): Any

}

/**
 * Companion object
 */
object RequestParamBinding {
  private val pathParamAnnotationType = ru.typeOf[RestPath]
  private val queryStringParamAnnotationType = ru.typeOf[RestQuery]
  private val headerParamAnnotationType = ru.typeOf[RestHeader]
  private val validParamAnnotationTypes = List(pathParamAnnotationType, queryStringParamAnnotationType, headerParamAnnotationType)
  private val optionType = ru.typeOf[Option[_]]

  private val standardTypes: Map[ru.Type, (String) => Any] = Map(
    (ru.typeOf[String], (s: String) => s),
    (ru.typeOf[Option[String]], (s: String) => Some(s)),
    (ru.typeOf[Int], (s: String) => s.toInt),
    (ru.typeOf[Option[Int]], (s: String) => Some(s.toInt)),
    (ru.typeOf[Boolean], (s: String) => s.toBoolean),
    (ru.typeOf[Option[Boolean]], (s: String) => Some(s.toBoolean)),
    (ru.typeOf[Byte], (s: String) => s.toByte),
    (ru.typeOf[Option[Byte]], (s: String) => Some(s.toByte)),
    (ru.typeOf[Short], (s: String) => s.toShort),
    (ru.typeOf[Option[Short]], (s: String) => Some(s.toShort)),
    (ru.typeOf[Long], (s: String) => s.toLong),
    (ru.typeOf[Option[Long]], (s: String) => Some(s.toLong)),
    (ru.typeOf[Double], (s: String) => s.toDouble),
    (ru.typeOf[Option[Double]], (s: String) => Some(s.toDouble)),
    (ru.typeOf[Float], (s: String) => s.toFloat),
    (ru.typeOf[Option[Float]], (s: String) => Some(s.toFloat)),
    (ru.typeOf[Date], (s: String) => DateUtil.parseISO8601Date(s)),
    (ru.typeOf[Option[Date]], (s: String) => if (s ==null || s.isEmpty()) None else Some(DateUtil.parseISO8601Date(s))))

  private val nameName = ru.newTermName("name")
  private val descriptionName = ru.newTermName("description")

  /**
   * Factory to create a parameter binding for a specific parameter in the constructor
   *
   * @param opDef Operation definition
   * @param requestClass Class to bind request data
   * @param p Parameter in the constructor of `requestClass`
   */
  def apply(
    opDef: RestOperationDef,
    requestClass: ru.ClassSymbol,
    p: ru.TermSymbol): RequestParamBinding = {

    val annotations = p.annotations

    // Check that there is only 1 parameter annotation
    val count = annotations.count(a => validParamAnnotationTypes.contains(a.tpe))
    if (count == 0) {
      throw RestDefintionException(s"Constructor parameter '${p.name}' of '${requestClass.fullName}' is not annotated." +
        "Annotated with PathParam, QueryStringParam or HeaderParam.")
    } else if (count > 1) {
      throw RestDefintionException(s"Constructor parameter '${p.name}' of '${requestClass.fullName}' has more than one REST annotation. " +
        "Only 1 REST annotation is permitted.")
    }

    // Parse annotation
    val a = annotations.find(a => validParamAnnotationTypes.contains(a.tpe)).get
    val name = ReflectUtil.getAnnotationJavaLiteralArg(a, nameName, p.name.toString())
    val description = ReflectUtil.getAnnotationJavaLiteralArg(a, descriptionName, "")
    val required = !(p.typeSignature <:< optionType)

    // Instance our binding class
    if (a.tpe =:= pathParamAnnotationType) {
      val idx = opDef.pathSegments.indexWhere(ps => ps.name == name && ps.isVariable)
      if (idx == -1) {
        throw RestDefintionException(s"Constructor parameter '${p.name}' of '${requestClass.fullName}' cannot be bound to the uri template path. " +
          s"'${opDef.uriTemplate}' does not contain variable named '${name}'.")
      }
      PathBinding(name, p.typeSignature, description, idx)

    } else if (a.tpe =:= queryStringParamAnnotationType) {
      QueryStringBinding(name, p.typeSignature, description, required)

    } else if (a.tpe =:= headerParamAnnotationType) {
      HeaderBinding(name, p.typeSignature, description, required)

    } else {
      throw new IllegalStateException("Unsupported annotation: " + a.tpe)
    }

  }

  /**
   * Parses a string and returns the required strictly types value
   *
   * @param s string to parse
   * @returns strictly typed value as specified in `tpe`
   */
  def parse(s: String, tpe: ru.Type): Any = {
    val entry = standardTypes.find(e => e._1 =:= tpe)
    if (entry.isDefined) {
      val (t, conversionFunc) = entry.get
      conversionFunc(s)
    } else {
      throw new RestBindingException("Unsupported type: " + tpe)
    }
  }
  

  
}

/**
 * Binds a value in the request class to a value in the request uri path
 */
case class PathBinding(name: String,
  tpe: ru.Type,
  description: String,
  pathIndex: Int) extends RequestParamBinding {

  val required = true

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext): Any = {
    val s = context.endPoint.pathSegments(pathIndex)
    if (s.isEmpty) {
      throw new RestBindingException(s"Cannot find path variable '${name}' in ${context.endPoint.path}")
    }
    RequestParamBinding.parse(s, tpe)
  }
}

/**
 * Binds a value in the request class to a value in the request query string
 */
case class QueryStringBinding(name: String,
  tpe: ru.Type,
  description: String,
  required: Boolean) extends RequestParamBinding {

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext): Any = {
    val s = context.endPoint.getQueryString(name)
    if (s.isEmpty || (s.isDefined && s.get.length == 0)) {
      if (required) {
        throw new RestBindingException(s"Cannot find query string variable '${name}' in ${context.endPoint.uri}")
      } else {
        // Must be an option because it is not required
        None
      }
    } else {
      RequestParamBinding.parse(s.get, tpe)
    }
  }
}

/**
 * Binds a value in the request class to a value in the request header
 */
case class HeaderBinding(name: String,
  tpe: ru.Type,
  description: String,
  required: Boolean) extends RequestParamBinding {

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext): Any = {
    val s = context.headers.get(name)
    if (s.isEmpty) {
      if (required) {
        throw new RestBindingException(s"Cannot find header variable '${name}' in ${context.headers}")
      } else {
        // Must be an option because it is not required
        None
      }
    } else {
      RequestParamBinding.parse(s.get, tpe)
    }
  }

}

  