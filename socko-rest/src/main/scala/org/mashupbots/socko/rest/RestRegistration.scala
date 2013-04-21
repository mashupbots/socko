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
import akka.actor.ActorSystem
import akka.actor.ActorRef
import java.util.Date

/**
 * Binds a [[org.mashupbots.socko.rest.RestRequest]], [[org.mashupbots.socko.rest.RestResponse]] and
 * a processor actor to an end point.
 *
 * This is implemented as an abstract class rather than a trait so that it is easier
 * to override methods and values.
 */
abstract class RestRegistration {

  /**
   * HTTP method associated with this operation
   */
  def method: Method.Value

  /**
   * Path template of this operation.
   *
   * Path is relative to the root path specified for the REST handler. For example, if the
   * REST handler path is `/api` and this path is `/my_operation`, then the full path to this
   * operation is `/api/my_operation`.
   *
   * The path may contain segments that are variable. For example, in `/pet/{id}`, `id` is a variable
   * and can be bound to a parameter in the [[org.mashupbots.socko.rest.RestRequest]].
   */
  def path: String

  /**
   * The type of the [[org.mashupbots.socko.rest.RestRequest]].
   *
   * If `None`, the default is the same name as the declaration class with `Declaration` replaced
   * with `Request`. For example, the default for `GetPetDeclaration` is `GetPetRequest.`
   */
  val request: Option[ru.Type] = None

  /**
   * Request parameter bindings.
   *
   * You can specify [[org.mashupbots.socko.rest.PathParam]], [[org.mashupbots.socko.rest.QueryParam]],
   * [[org.mashupbots.socko.rest.HeaderParam]] and/or [[org.mashupbots.socko.rest.BodyParam]].
   */
  def requestParams: Seq[RequestParam]

  /**
   * The type of the [[org.mashupbots.socko.rest.RestResponse]].
   *
   * If `None`, the default is the same name as the declaration class with `Declaration` replaced
   * with `Response`. For example, the default for `GetPetDeclaration` is `GetPetResponse.`
   */
  val response: Option[ru.Type] = None

  /**
   * Locates the actor that will process a request and returns a response.
   *
   * @param actorSystem Actor system in which new actors maybe created
   * @param request Rest Request to process
   * @return `ActorRef` of actor to which `request` will be sent for processing
   */
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef

  /**
   * Flag to denote if the [[org.mashupbots.socko.events.SockoEvent]] is to be made
   * accessible from [[org.mashupbots.socko.rest.RestRequestEvents]] so that the REST processing
   * actor can access the raw request data for custom deserialization.
   *
   * Defaults to `false`.
   */
  val customDeserialization: Boolean = false

  /**
   * Flag to denote if the [[org.mashupbots.socko.events.SockoEvent]] is to be made
   * accessible from [[org.mashupbots.socko.rest.RestRequestEvents]] so that the REST processing
   * actor can write data directory to the client. Defaults to `false`.
   */
  val customSerialization: Boolean = false

  /**
   * Name of this operation.
   *
   * If empty, the default is the name of the declaration class without `Declaration.`
   * For example, the default name for `UpdatePetDeclaration` is `UpdatePet`.
   */
  val name: String = ""

  /**
   * Short description. Less than 60 characters is recommended.
   *
   * Default is blank.
   */
  val description = ""

  /**
   * Long description.
   *
   * Default is blank.
   */
  val notes = ""

  /**
   * Flag to denote if this operation is deprecated.
   *
   * Programmers are discouraged from using this operation because it is dangerous, or because a
   * better alternative exists. The default is `false`.
   */
  val deprecated: Boolean = false

  /**
   * Details of possible errors
   */
  val errors: Seq[Error] = Seq.empty

}

/**
 * HTTP method supported by [[org.mashupbots.socko.rest.RestHandler]]
 */
object Method extends Enumeration {
  type Method = Value
  val GET, POST, PUT, DELETE = Value
}

/**
 * Details a HTTP error
 *
 * @param code HTTP response status code
 * @param reason Text description of the error
 */
case class Error(code: Int, reason: String)

/**
 * Identifies a [[org.mashupbots.socko.rest.RestRequest]] parameter binding
 *
 * @param name Name of the request parameter
 * @param description Short description of the parameter
 * @param allowMultiple Specifies that a comma-separated list of values can be passed
 *   Defaults to `false`.
 * @param allowableValues Input validation
 */
trait RequestParam {
  def name: String
  def description: String
  def allowMultiple: Boolean
  def allowableValues: Option[AllowableValues]
  def paramType: String
}

/**
 * Parameter that binds to a value in the path
 *
 * @param name Name of the request parameter
 * @param description Short description of the parameter
 * @param allowableValues Input validation
 */
case class PathParam(
  name: String,
  description: String = "",
  allowableValues: Option[AllowableValues] = None) extends RequestParam {

  val paramType = "path"
  val allowMultiple: Boolean = false
}

/**
 * Parameter that binds to a value in the query string
 *
 * @param name Name of the request parameter
 * @param queryName Name of the query string field. If empty (default), then the query string field
 *   name is assumed to be the same as `name`.
 * @param description Short description of the parameter
 * @param allowMultiple Specifies that a comma-separated list of values can be passed
 *   Defaults to `false`.
 * @param allowableValues Input validation
 */
case class QueryParam(
  name: String,
  description: String = "",
  queryName: String = "",
  allowMultiple: Boolean = false,
  allowableValues: Option[AllowableValues] = None) extends RequestParam {

  val paramType = "query"
}

/**
 * Parameter that binds to a value in the header
 *
 * @param name Name of the request parameter
 * @param headerName Name of the header field. If empty (default), then the header field name
 *   is assumed to be the same as `name`.
 * @param description Short description of the parameter
 * @param allowMultiple Specifies that a comma-separated list of values can be passed.
 *   Defaults to `false`.
 * @param allowableValues Input validation
 */
case class HeaderParam(
  name: String,
  description: String = "",
  headerName: String = "",
  allowMultiple: Boolean = false,
  allowableValues: Option[AllowableValues] = None) extends RequestParam {

  val paramType = "header"
}

/**
 * Parameter that binds to a value in the header
 *
 * @param name Name of the request parameter
 * @param description Short description of the parameter
 */
case class BodyParam(
  name: String,
  description: String = "") extends RequestParam {

  val paramType = "body"
  val allowableValues: Option[AllowableValues] = None
  val allowMultiple: Boolean = false
}

/**
 * Identifies a [[org.mashupbots.socko.rest.RestRequest]] parameter validation
 * 
 * Note that `valueType` must be in the constructor otherwise it will not be be 
 * json serialized.
 */
trait AllowableValues
case class AllowableValuesRange[T](min: T, max: T, valueType: String = "RANGE") extends AllowableValues
case class AllowableValuesList[T](values: List[T], valueType: String = "LIST") extends AllowableValues

/**
 * Companion object
 */
object AllowableValues {
  def apply[T](min: T, max: T): AllowableValues = AllowableValuesRange(min, max)
  def apply[T](values: List[T]): AllowableValues = AllowableValuesList(values)
}
