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
package org.mashupbots.socko.rest.test1

import scala.reflect.runtime.{ universe => ru }

import org.mashupbots.socko.rest.Error
import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.QueryParam
import org.mashupbots.socko.rest.RestRegistration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.ActorRef
import akka.actor.ActorSystem

case class GetPetsRequest(context: RestRequestContext) extends RestRequest
case class GetPetsResponse(context: RestResponseContext) extends RestResponse
object GetPetsRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class PostDogs1Request(context: RestRequestContext) extends RestRequest
object PostDogs1Registration extends RestRegistration {
  val method = Method.POST
  val path = "/dogs1"
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
  val requestParams = Seq.empty
  override val response = Some(ru.typeOf[FunnyNameDogResponse])
}

case class PutDogs2Request(context: RestRequestContext) extends RestRequest
object PutDogs2Registration extends RestRegistration {
  val method = Method.PUT
  val path = "/dogs2"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
  override val response = Some(ru.typeOf[FunnyNameDogResponse])
  override val errors = Seq(Error(400, "username not found"), Error(401, "yet another error"))
}

case class FunnyNameDogResponse(context: RestResponseContext) extends RestResponse

case class DeletePetsRequest(context: RestRequestContext, id: String) extends RestRequest
case class DeletePetsResponse(context: RestResponseContext, message: String) extends RestResponse
object DeletePetsRegistration extends RestRegistration {
  val method = Method.DELETE
  val path = "/pets/{id}"
  val requestParams = Seq(PathParam("id"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error because there is no corresponding response class
case class NoResponseRequest(context: RestRequestContext) extends RestRequest
object NoResponseRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/noresponse"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error because parameter binding specified
case class NoParameterRequest(context: RestRequestContext, id: String) extends RestRequest
case class NoParameterResponse(context: RestResponseContext) extends RestResponse
object NoParameterRegistration extends RestRegistration {
  val method = Method.DELETE
  val path = "/pets/{id}"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error because parameter bound more than one
case class MultiParameterRequest(context: RestRequestContext, id: String) extends RestRequest
case class MultiParameterResponse(context: RestResponseContext) extends RestResponse
object MultiParameterRegistration extends RestRegistration {
  val method = Method.DELETE
  val path = "/pets/{id}"
  val requestParams = Seq(PathParam("id"), QueryParam("id"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Ignored because not a RestRequest
case class NotARestClass()

