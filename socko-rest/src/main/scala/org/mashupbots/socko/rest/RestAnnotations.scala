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

import java.util.Date

//*********************************************************************************************************************
// Operations
//*********************************************************************************************************************

/**
 * Standard definitions for our operations
 */
trait RestOperation {
  
  /**
   * HTTP method
   */
  def method: String
  
  
}


/**
 * HTTP GET REST end point annotation
 *
 * ==Example Usage==
 * Simple GET.
 * {{{
 * @RestGet(
 *   uriTemplate = "/pets",
 *   actorPath = "/my/actor/path"
 * )
 * case class GetPetRequest()
 * }}}
 * 
 * Simple GET with lookup of actor path at bootup time
 * {{{
 * @RestGet(
 *   uriTemplate = "/pets",
 *   actorPath = "lookup:mykey"
 * )
 * case class GetPetRequest()
 * }}}
 * 
 * Get with a path parameter.
 * {{{
 * @RestGet(
 *   uriTemplate = "/pet/{petId}",
 *   actorPath = "/my/actor/path",
 *   description = "Retrieves details of the specified pet"
 * )
 * case class GetPetsRequest(
 *   @PathParam(
 *     description = "ID of pet that needs to be fetched"
 *   )
 *   petId: String
 * )
 * }}} 
 *  
 * @param uriTemplate Template URI.
 * @param actorPath Path to actor to which this request will be sent for processing.
 *   You can also bind your request to an actor at bootup time using the `lookup:{key}` prefix.
 *   The `key` is the key to a map of actor names passed into the request processor.
 * @param responseClass Optional class path of the response class. Defaults to the same class path and name
 *   as the request class; but with `Request` suffix replaced with `Response`. For `MyRestRequest`, the default
 *   response class would be `MyRestResponse`.
 * @param name Optional field provided by the server for the convenience of the UI and client code generator.
 *   Defaults to the name of the request class without the `Request` suffix.
 * @param description Optional short description. Less than 60 characters is recommended.
 * @param notes Optional long description
 * @param depreciated Flag to indicate if this endpoint is depreciated or not. Defaults to `false`.
 * @param errorResponses Optional map of error codes and descriptions
 */
case class RestGet(
  uriTemplate: String,
  actorPath: String,
  responseClass: String = "",
  name: String = "",
  description: String = "",
  notes: String = "",
  depreciated: Boolean = false,
  errorResponses: Map[String, String] = Map.empty) extends scala.annotation.StaticAnnotation with RestOperation {
  
  val method: String = "GET"
    
}

//*********************************************************************************************************************
// Parameters
//*********************************************************************************************************************

trait AllowableValues

/**
 * Allowable string values
 */
case class AllowableStringValues(
  min: String = "",
  max: String = "",
  values: List[String] = List.empty) extends AllowableValues

/**
 * Allowable integer values
 */
case class AllowableIntValues(
  min: Option[Int] = None,
  max: Option[Int] = None,
  values: List[Int] = List.empty) extends AllowableValues

/**
 * Allowable long integer values
 */
case class AllowableLongValues(
  min: Option[Long] = None,
  max: Option[Long] = None,
  values: List[Long] = List.empty) extends AllowableValues

/**
 * Allowable float point values
 */
case class AllowableFloatValues(
  min: Option[Float] = None,
  max: Option[Float] = None,
  values: List[Float] = List.empty) extends AllowableValues
  
/**
 * Allowable double floating point values
 */
case class AllowableDoubleValues(
  min: Option[Double] = None,
  max: Option[Double] = None,
  values: List[Double] = List.empty) extends AllowableValues

/**
 * Allowable date values
 */
case class AllowableDateValues(
  min: Option[Date] = None,
  max: Option[Date] = None,
  values: List[Date] = List.empty) extends AllowableValues

/**
 * Path parameter annotation. Binds this parameter a value specified on the URI template. 
 *
 * ==Example Usage==
 * {{{
 * @RestGet(
 *   uriTemplate = "/pet/{petId}",
 *   actorPath = "/my/actor/path",
 *   description = "Retrieves details of the specified pet"
 * )
 * case class GetPetRequest(
 *   @PathParam(
 *     description = "ID of pet that needs to be fetched"
 *   )
 *   petId: String
 * )
 * }}}
 * 
 * @param name Optional name of the parameter. Defaults to the bound variable name.  Must match a variable
 *   specified in the URI template of the operation.
 * @param description Optional description of this parameter
 * @param allowableValues Optional list of allowable values to restrict the inputs for this field
 */
case class PathParam(
  name: String = "",
  description: String = "",
  allowableValues: Option[AllowableValues] = None) extends scala.annotation.StaticAnnotation
  
/**
 * Query string parameter annotation. Binds this parameter a value specified in the HTTP request query string. 
 *
 * ==Example Usage==
 * {{{
 * @RestGet(
 *   uriTemplate = "/pets",
 *   actorPath = "/my/actor/path",
 *   description = "Retrieves a list of pets"
 * )
 * case class GetPetRequest(
 *   @QueryParam(
 *     description = "Name of the owner of the pet"
 *   )
 *   owner: Option[String],
 * 
 *   @QueryParam(
 *     name = "r",
 *     description = "Maximum number of rows to return"
 *   )
 *   rows: Option[Int]
 * 
 * )
 * }}}
 * 
 * The following URI requests will be valid:
 * {{{
 * http://mydomain.com/pets
 * http://mydomain.com/pets?owner=Jim
 * http://mydomain.com/pets?owner=Jack&r=50
 * }}}
 * 
 * @param name Optional name of the query string field. Defaults to the bound variable name.  This must match
 *   the query string field name to which you wish to bind.
 * @param description Optional description of this parameter
 * @param required Optional flag to denote if this query string parameter is required or not. Defaults to `false`.
 *   If false, the bound variable must be an option.
 * @param allowableValues Optional list of allowable values to restrict the inputs for this field
 */
case class QueryParam(
  name: String = "",
  description: String = "",
  required: Boolean = false,
  allowableValues: Option[AllowableValues] = None) extends scala.annotation.StaticAnnotation
  
/**
 * Request header parameter annotation. Binds this parameter a value specified in the HTTP request message header
 *
 * ==Example Usage==
 * {{{
 * @RestGet(
 *   uriTemplate = "/pets",
 *   actorPath = "/my/actor/path",
 *   description = "Retrieves a list of pets"
 * )
 * case class GetPetRequest(
 *   @HeaderParam(
 *     description = "Session id obtained at login"
 *   )
 *   sessionId: String
 * )
 * }}}
 * 
 * @param name Optional name of the header field. Defaults to the bound variable name. This must match
 *   the HTTP header name to which you wish to bind.
 * @param description Optional description of this parameter
 * @param required Optional flag to denote if this query string parameter is required or not. Defaults to `false`
 *   If false, the bound variable must be an option.
 * @param allowableValues Optional list of allowable values to restrict the inputs for this field
 */
case class HeaderParam(
  name: String = "",
  description: String = "",
  required: Boolean = false,
  allowableValues: Option[AllowableValues] = None) extends scala.annotation.StaticAnnotation
  
/**
 * Request body annotation. Binds this parameter to the data posted in the HTTP request message body
 *
 * @param name Optional name of the body field. Defaults to the bound variable name.
 *   This is only used for code generation and UI.
 * @param description Optional description of this parameter
 * @param required Optional flag to denote if this query string parameter is required or not. Defaults to `false`
 *   If false, the bound variable must be an option.
 */
case class BodyParam(
  name: String = "",
  description: String = "",
  required: Boolean = false,
  allowableValues: Option[AllowableValues] = None) extends scala.annotation.StaticAnnotation  
  