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
import java.util.UUID

import org.mashupbots.socko.events.EndPoint
import org.mashupbots.socko.events.HttpResponseStatus

/**
 * Provides context to a REST request. Contains request meta-data.
 *
 * @param id UUID for this rest request/response pair
 * @param endPoint HTTP URL at which the request was received
 * @param headers HTTP request headers
 * @param timeoutSeconds Number of seconds before this request times out
 */
case class RestRequestContext(
  id: UUID,
  endPoint: EndPoint,
  headers: Map[String, String],
  eventType: SockoEventType.Value,
  timeoutSeconds: Int) {

  val startTime = new Date()
  val timeoutTime = new Date(startTime.getTime() + (timeoutSeconds * 1000))

  /**
   * Alternative constructor that automatically allocates the unique Id
   *
   * @param endPoint HTTP URL at which the request was received
   * @param headers HTTP request headers
   * @param timeoutSeconds Number of seconds before this request times out
   */
  def this(endPoint: EndPoint,
    headers: Map[String, String],
    eventType: SockoEventType.Value,
    timeoutSeconds: Int) =
    this(UUID.randomUUID(), endPoint, headers, eventType, timeoutSeconds)

  /**
   * Builds the [[org.mashupbots.socko.rest.RestResponseContext]] using the details of this
   * [[org.mashupbots.socko.rest.RestRequestContext]].
   *
   * It is assumed that there are no response headers and the response status is `200` OK.
   *
   * @return [[org.mashupbots.socko.rest.RestResponseContext]] using the details of this context
   */
  def responseContext(): RestResponseContext = {
    RestResponseContext(this, HttpResponseStatus(200), Map.empty)
  }
  
  /**
   * Builds the [[org.mashupbots.socko.rest.RestResponseContext]] using the details of this
   * [[org.mashupbots.socko.rest.RestRequestContext]].
   *
   * It is assumed that there are no response headers.
   *
   * @param status HTTP status of the response
   * @return [[org.mashupbots.socko.rest.RestResponseContext]] using the details of this context
   */
  def responseContext(status: Int): RestResponseContext = {
    RestResponseContext(this, HttpResponseStatus(status), Map.empty)
  }

  /**
   * Builds the [[org.mashupbots.socko.rest.RestResponseContext]] using the details of this
   * [[org.mashupbots.socko.rest.RestRequestContext]].
   *
   * @param status HTTP status of the response
   * @param headers HTTP response headers
   * @return [[org.mashupbots.socko.rest.RestResponseContext]] using the details of this context
   */
  def responseContext(status: Int, headers: Map[String, String]): RestResponseContext = {
    RestResponseContext(this, HttpResponseStatus(status), headers)
  } 
  
}

/**
 * Companion object
 */
object RestRequestContext {

  /**
   * Factory to instance a new [[org.mashupbots.socko.rest.RestRequestContext]] with a random
   * UUID.
   *
   * @param endPoint HTTP URL at which the request was received
   * @param headers HTTP request headers
   * @param eventType Socko Event Type
   * @param timeoutSeconds Number of seconds before this request times out
   * @return a new instance of [[org.mashupbots.socko.rest.RestRequestContext]]
   */
  def apply(endPoint: EndPoint,
    headers: Map[String, String],
    eventType: SockoEventType.Value,
    timeoutSeconds: Int): RestRequestContext = {
    RestRequestContext(UUID.randomUUID(), endPoint, headers, eventType, timeoutSeconds)
  }
}

/**
 * Denotes the type of [[org.mashupbots.socko.events.SockoEvent]] that triggered this
 * REST request
 */
object SockoEventType extends Enumeration {
  type SockoEventType = Value

  /**
   * HTTP
   */
  val HttpRequest = Value

  /**
   * Web Sockets
   */
  val WebSocketFrame = Value
} 
  
