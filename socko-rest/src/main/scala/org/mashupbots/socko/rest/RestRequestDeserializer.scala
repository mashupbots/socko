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

import java.util.Date

import scala.reflect.runtime.{ universe => ru }

import org.json4s.NoTypeHints
import org.json4s.native.{ Serialization => json }
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.ReflectUtil

/**
 * Deserializes incoming data into a [[org.mashupbots.socko.rest.RestRequest]]
 *
 * @param config REST config
 * @param requestClass Request class
 * @param requestConstructor Constructor to call when instancing the request class
 * @param params Bindings to extract values form the request data. The values will be
 *   passed into the requestConstructor to instance the request class.
 */
case class RestRequestDeserializer(
  config: RestConfig,
  requestClass: ru.ClassSymbol,
  requestConstructorMirror: ru.MethodMirror,
  requestParamBindings: List[RequestParamBinding]) {

  /**
   * Deserialize a [[org.mashupbots.socko.rest.RestRequest]] from a HTTP request event.
   * This method is just use in unit testing
   *
   * @param context Context of the HTTP request
   * @param httpRequestEvent HTTP event
   */
  def deserialize(context: RestRequestContext): RestRequest = {
    val params: List[_] = context :: requestParamBindings.map(b => b.extract(context, requestClass, null))
    requestConstructorMirror(params: _*).asInstanceOf[RestRequest]
  }

  /**
   * Deserialize a [[org.mashupbots.socko.rest.RestRequest]] from a HTTP request event
   *
   * @param httpRequestEvent HTTP event
   */
  def deserialize(httpRequestEvent: HttpRequestEvent): RestRequest = {
    val context = RestRequestContext(
        httpRequestEvent.endPoint, 
        httpRequestEvent.request.headers, 
        SockoEventType.HttpRequest,
        config.requestTimeoutSeconds)
    val params: List[_] = context :: requestParamBindings.map(b => b.extract(context, requestClass, httpRequestEvent))
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
   * Factory for RestRequestDeserializer
   *
   * @param config Rest configuration
   * @param rm Runtime Mirror with the same class loaders as the specified request class
   * @param definition Definition of the operation
   * @param requestClassSymbol Request class symbol
   */
  def apply(config: RestConfig, rm: ru.Mirror, definition: RestOperationDef, requestClassSymbol: ru.ClassSymbol): RestRequestDeserializer = {
    val requestConstructor: ru.MethodSymbol = requestClassSymbol.toType.declaration(ru.nme.CONSTRUCTOR).asMethod
    val requestConstructorMirror: ru.MethodMirror = rm.reflectClass(requestClassSymbol).reflectConstructor(requestConstructor)

    val requestConstructorParams: List[ru.TermSymbol] = requestConstructor.paramss(0).map(p => p.asTerm)

    // First param better be  RestRequestContext
    if (requestConstructorParams.head.typeSignature != restRequestContextType) {
      throw RestDefintionException(s"First constructor parameter of '${requestClassSymbol.fullName}' must be of type RestRequestContext.")
    }
    if (requestConstructorParams.head.name.toString != "context") {
      throw RestDefintionException(s"First constructor parameter of '${requestClassSymbol.fullName}' must be called 'context'.")
    }

    val params = requestConstructorParams.tail.map(p => RequestParamBinding(config, definition, requestClassSymbol, p))

    RestRequestDeserializer(config, requestClassSymbol, requestConstructorMirror, params)
  }

}

/**
 * Binding of a request value
 */
trait RequestParamBinding {

  /**
   * REST configuration
   */
  def config: RestConfig

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
   * @param requestClass Request class to use in error messages
   * @param httpRequestEvent HTTP request event
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext, requestClass: ru.ClassSymbol, httpRequestEvent: HttpRequestEvent): Any
}

/**
 * Companion object
 */
object RequestParamBinding {
  private val pathParamAnnotationType = ru.typeOf[RestPath]
  private val queryStringParamAnnotationType = ru.typeOf[RestQuery]
  private val headerParamAnnotationType = ru.typeOf[RestHeader]
  private val bodyParamAnnotationType = ru.typeOf[RestBody]
  private val validParamAnnotationTypes = List(pathParamAnnotationType,
    queryStringParamAnnotationType, headerParamAnnotationType, bodyParamAnnotationType)
  private val optionType = ru.typeOf[Option[_]]

  private val bytesType = ru.typeOf[Seq[Byte]]
  private val anytRefType = ru.typeOf[AnyRef]

  val primitiveTypes: Map[ru.Type, (String) => Any] = Map(
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
    (ru.typeOf[Option[Date]], (s: String) => if (s == null || s.isEmpty()) None else Some(DateUtil.parseISO8601Date(s))))

  private val nameName = ru.newTermName("name")
  private val descriptionName = ru.newTermName("description")

  /**
   * Factory to create a parameter binding for a specific parameter in the constructor
   *
   * @param config REST config
   * @param opDef Operation definition
   * @param requestClass Class to bind request data
   * @param p Parameter in the constructor of `requestClass`
   */
  def apply(
    config: RestConfig,
    opDef: RestOperationDef,
    requestClass: ru.ClassSymbol,
    p: ru.TermSymbol): RequestParamBinding = {

    val annotations = p.annotations

    // Check that there is only 1 parameter annotation
    val count = annotations.count(a => validParamAnnotationTypes.contains(a.tpe))
    if (count == 0) {
      throw RestDefintionException(s"Constructor parameter '${p.name}' of '${requestClass.fullName}' is not annotated " +
        "with @RestPath, @RestQuery, @RestHeader or @RestBody")
    } else if (count > 1) {
      throw RestDefintionException(s"Constructor parameter '${p.name}' of '${requestClass.fullName}' has more than one REST annotation")
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
          s"'${opDef.urlTemplate}' does not contain variable named '${name}'.")
      }
      PathBinding(config, name, p.typeSignature, description, idx)
    } else if (a.tpe =:= queryStringParamAnnotationType) {
      QueryStringBinding(config, name, p.typeSignature, description, required)
    } else if (a.tpe =:= headerParamAnnotationType) {
      HeaderBinding(config, name, p.typeSignature, description, required)
    } else if (a.tpe =:= bodyParamAnnotationType) {
      if (opDef.method != "PUT" && opDef.method != "POST") {
        throw RestDefintionException(s"Constructor parameter '${p.name}' of '${requestClass.fullName}' cannot be bound using @RestBody() for a '${opDef.method}' operation.")        
      }
      
      val tpe = p.typeSignature
      val tpeCategory = if (primitiveTypes.exists(t => t._1 =:= tpe)) {
        RequestBodyDataType.Primitive
      } else if (tpe =:= bytesType) {
        RequestBodyDataType.Bytes
      } else if (tpe <:< anytRefType) {
        RequestBodyDataType.Object
      } else {
        throw new IllegalArgumentException(s"Unsupported REST request body data type ${tpe} in ${requestClass.fullName}.")
      }

      val objectClass = if (tpeCategory == RequestBodyDataType.Object) {
        if (required) Some(Class.forName(p.typeSignature.typeSymbol.asClass.fullName))
        else {
          // Extract underlying type from Option to help with deserlialization
          import ru._ // Remove unchecked warning: https://issues.scala-lang.org/browse/SI-6338
          val targs = p.typeSignature match { case ru.TypeRef(_, _, args) => args }
          Some(Class.forName(targs(0).typeSymbol.asClass.fullName))
        }
      } else {
        None
      }

      BodyBinding(config, name, tpeCategory, tpe, objectClass, description, required)
    } else {
      throw new IllegalStateException("Unsupported annotation: " + a.tpe)
    }
  }
}

/**
 * Path, QueryString and Header params must bind to a primitive.  This trait holds their common functions.
 */
trait PrimitiveParamBinding extends RequestParamBinding {

  /**
   * Parse a string into the specified
   *
   * We load this at intialization so it is done once.
   */
  val primitiveParser: (String) => Any = {
    val entry = RequestParamBinding.primitiveTypes.find(e => e._1 =:= tpe)
    if (entry.isDefined) {
      val (t, conversionFunc) = entry.get
      conversionFunc
    } else {
      throw new RestBindingException("Unsupported type: " + tpe)
    }
  }

}

/**
 * Binds a value in the request class to a value in the request uri path
 *
 * ==Example==
 * {{{
 * /path/{Id}
 * case class(context: RestRequestContext, @RestPath() id: Int) extends RestRequest
 * }}}
 *  - name = id
 *  - tpe = Int
 *  - description = ""
 *  - pathIndex = 1
 *
 * @param config REST config
 * @param name Name of the field in the [[org.mashupbots.socko.rest.RestRequest]] to bind data to
 * @param tpe Type of the field
 * @param description Description of the field
 * @param pathIndex Index of the value of the field in array of path segments
 */
case class PathBinding(
  config: RestConfig,
  name: String,
  tpe: ru.Type,
  description: String,
  pathIndex: Int) extends PrimitiveParamBinding {

  val required = true

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @param requestClass Request class to use in error messages
   * @param httpRequestEvent HTTP request event
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext, requestClass: ru.ClassSymbol, httpRequestEvent: HttpRequestEvent): Any = {
    val s = context.endPoint.pathSegments(pathIndex)
    if (s.isEmpty) {
      throw new RestBindingException(s"Cannot find path variable '${name}' in '${context.endPoint.path}' for request '${requestClass.fullName}'")
    }
    try {
      primitiveParser(s)
    } catch {
      case e: Throwable =>
        throw RestBindingException(s"Cannot parse '${s}' for path variable '${name}' in '${context.endPoint.path}' for request '${requestClass.fullName}'", e)
    }
  }
}

/**
 * Binds a value in the request class to a value in the request query string
 *
 * ==Example==
 * {{{
 * /path?rows=1
 * case class(context: RestRequestContext, @RestQuery() rows: Option[Int]) extends RestRequest
 * }}}
 *  - name = rows
 *  - tpe = Int
 *  - description = ""
 *  - required = false
 *
 * @param config REST config
 * @param name Name of the field in the [[org.mashupbots.socko.rest.RestRequest]] to bind data to
 * @param tpe Type of the field
 * @param description Description of the field
 * @param required Flag to indicate if this field is required or not. If not, it must be of type `Option[_]`
 */
case class QueryStringBinding(
  config: RestConfig,
  name: String,
  tpe: ru.Type,
  description: String,
  required: Boolean) extends PrimitiveParamBinding {

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @param requestClass Request class to use in error messages
   * @param httpRequestEvent HTTP request event
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext, requestClass: ru.ClassSymbol, httpRequestEvent: HttpRequestEvent): Any = {
    val s = context.endPoint.getQueryString(name)
    if (s.isEmpty || (s.isDefined && s.get.length == 0)) {
      if (required) {
        throw new RestBindingException(s"Cannot find query string variable '${name}' in '${context.endPoint.uri}' for request '${requestClass.fullName}'")
      } else {
        // Must be an option because it is not required
        None
      }
    } else {
      try {
        primitiveParser(s.get)
      } catch {
        case e: Throwable =>
          throw RestBindingException(s"Cannot parse '${s}' for query string variable '${name}' in '${context.endPoint.uri}' for request '${requestClass.fullName}'", e)
      }
    }
  }
}

/**
 * Binds a value in the request class to a value in the request header
 *
 * ==Example==
 * {{{
 * case class(context: RestRequestContext, @RestHeader() rows: Int) extends RestRequest
 * }}}
 *  - name = rows
 *  - tpe = Int
 *  - description = ""
 *  - required = true
 *
 * @param config REST config
 * @param name Name of the field in the [[org.mashupbots.socko.rest.RestRequest]] to bind data to
 * @param tpe Type of the field
 * @param description Description of the field
 * @param required Flag to indicate if this field is required or not. If not, it must be of type `Option[_]`
 */
case class HeaderBinding(
  config: RestConfig,
  name: String,
  tpe: ru.Type,
  description: String,
  required: Boolean) extends PrimitiveParamBinding {

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @param requestClass Request class to use in error messages
   * @param httpRequestEvent HTTP request event
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext, requestClass: ru.ClassSymbol, httpRequestEvent: HttpRequestEvent): Any = {
    val s = context.headers.get(name)
    if (s.isEmpty) {
      if (required) {
        throw new RestBindingException(s"Cannot find header variable '${name}' for request '${requestClass.fullName}'")
      } else {
        // Must be an option because it is not required
        None
      }
    } else {
      try {
        primitiveParser(s.get)
      } catch {
        case e: Throwable =>
          throw RestBindingException(s"Cannot parse '${s}' for header variable '${name}' for request '${requestClass.fullName}'", e)
      }
    }
  }
}

/**
 * Binds a value in the request class to a value in the request body
 *
 * ==Example==
 * {{{
 * case class(context: RestRequestContext, @RestBody() pet: Pet) extends RestRequest
 * }}}
 *  - name = pet
 *  - tpe = Pet
 *  - clz = Class.forName("my.package.Pet")
 *  - description = ""
 *  - required = true
 *
 * @param config REST config
 * @param name Name of the field in the [[org.mashupbots.socko.rest.RestRequest]] to bind data to
 * @param tpeCategory Our categorization of the type of the field
 * @param tpe Type of the field
 * @param objectClass For object fields that needs to be deserialized, this is the Java class of the field.
 *   For other categories of deserialization (primitive, bytes, etc) this is set to `None` and not used.
 * @param description Description of the field
 * @param required Flag to indicate if this field is required or not. If not, it must be of type `Option[_]`
 */
case class BodyBinding(
  config: RestConfig,
  name: String,
  tpeCategory: RequestBodyDataType.Value,
  tpe: ru.Type,
  objectClass: Option[Class[_]],
  description: String,
  required: Boolean) extends RequestParamBinding {

  /**
   * Parse a string into the specified
   *
   * We load this at intialization so it is done once.
   */
  val primitiveParser: Option[(String) => Any] = {
    if (tpeCategory == RequestBodyDataType.Primitive) {
      val entry = RequestParamBinding.primitiveTypes.find(e => e._1 =:= tpe)
      if (entry.isDefined) {
        val (t, conversionFunc) = entry.get
        Some(conversionFunc)
      } else {
        throw new RestBindingException("Unsupported type: " + tpe)
      }
    } else {
      None
    }
  }

  /**
   * Parse incoming request data into a value for binding to a [[org.mashupbots.socko.rest.RequestClass]]
   *
   * @param context Request context
   * @param requestClass Request class to use in error messages
   * @param httpRequestEvent HTTP request event
   * @returns a value for passing to the constructor
   */
  def extract(context: RestRequestContext, requestClass: ru.ClassSymbol, httpRequestEvent: HttpRequestEvent): Any = {
    tpeCategory match {
      case RequestBodyDataType.Object =>
        val s = httpRequestEvent.request.content.toString
        if (s.isEmpty) {
          if (required) {
            throw new RestBindingException(s"Request body is empty for request '${requestClass.fullName}'")
          } else {
            // Must be an option because it is not required
            None
          }
        } else {
          try {
            val formats = json.formats(NoTypeHints)
            val scalaType = org.json4s.reflect.Reflector.scalaTypeOf(objectClass.get)
            val scalaManifest = org.json4s.reflect.ManifestFactory.manifestOf(scalaType)
            val data = json.read(s)(formats, scalaManifest)
            if (required) data
            else Some(data)
          } catch {
            case e: Throwable =>
              throw RestBindingException(s"Cannot parse '${s}' for body '${name}' for request '${requestClass.fullName}'", e)
          }
        }
      case RequestBodyDataType.Primitive =>
        val s = httpRequestEvent.request.content.toString
        if (s.isEmpty) {
          if (required) {
            throw new RestBindingException(s"Cannot bind empty body for request '${requestClass.fullName}'")
          } else {
            // Must be an option because it is not required
            None
          }
        } else {
          try {
            primitiveParser.get(s)
          } catch {
            case e: Throwable =>
              throw RestBindingException(s"Cannot parse '${s}' for body for request '${requestClass.fullName}'", e)
          }
        }
      case RequestBodyDataType.Bytes =>
        httpRequestEvent.request.content.toBytes.toSeq

      case _ => throw RestBindingException(s"Unsupported request body binding type category: ${tpeCategory}")
    }
  }
}

object RequestBodyDataType extends Enumeration {
  type RequestBodyDataType = Value
  val Primitive, Object, Bytes = Value
} 
  