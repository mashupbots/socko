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
package org.mashupbots.socko.rest.petshop

import org.mashupbots.socko.rest.BodyParam
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

case class User(
  id: Long,
  lastName: String,
  phone: String,
  username: String,
  email: String,
  userStatus: Int,
  firstName: String,
  password: String)

case class CreateUserWithArrayRequest(context: RestRequestContext, users: Array[User]) extends RestRequest
case class CreateUserWithArrayResponse(context: RestResponseContext) extends RestResponse
object CreateUserWithArrayRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/user/createWithArray"
  val requestParams = Seq(BodyParam("users"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class CreateUserRequest(context: RestRequestContext, user: User) extends RestRequest
case class CreateUserResponse(context: RestResponseContext) extends RestResponse
object CreateUserRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/user"
  val requestParams = Seq(BodyParam("user"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class CreateUserWithListRequest(context: RestRequestContext, users: Seq[User]) extends RestRequest
case class CreateUserWithListResponse(context: RestResponseContext) extends RestResponse
object CreateUserWithListRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/user/createWithList"
  val requestParams = Seq(BodyParam("users"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class UpdateUserRequest(context: RestRequestContext, username: String, user: User) extends RestRequest
case class UpdateUserResponse(context: RestResponseContext) extends RestResponse
object UpdateUserRegistration extends RestRegistration {
  val method = Method.PUT
  val path = "/user/{username}"
  val requestParams = Seq(PathParam("username"), BodyParam("user"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class DeleteUserRequest(context: RestRequestContext, username: String) extends RestRequest
case class DeleteUserResponse(context: RestResponseContext) extends RestResponse
object DeleteUserRegistration extends RestRegistration {
  val method = Method.DELETE
  val path = "/user/{username}"
  val requestParams = Seq(PathParam("username"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class GetUserRequest(context: RestRequestContext, username: String) extends RestRequest
case class GetUserResponse(context: RestResponseContext, user: Option[User]) extends RestResponse
object GetUserRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/user/{username}"
  val requestParams = Seq(PathParam("username"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class LoginRequest(context: RestRequestContext, username: String, password: String) extends RestRequest
case class LoginResponse(context: RestResponseContext, result: String) extends RestResponse
object LoginRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/user/login"
  val requestParams = Seq(QueryParam("username"), QueryParam("password"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class LogoutRequest(context: RestRequestContext) extends RestRequest
case class LogoutResponse(context: RestResponseContext) extends RestResponse
object LogoutRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/user/logout"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}


