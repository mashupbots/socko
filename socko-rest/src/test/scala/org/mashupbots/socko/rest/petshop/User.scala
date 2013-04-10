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

import org.mashupbots.socko.rest.RestBody
import org.mashupbots.socko.rest.RestDelete
import org.mashupbots.socko.rest.RestDispatcher
import org.mashupbots.socko.rest.RestGet
import org.mashupbots.socko.rest.RestPath
import org.mashupbots.socko.rest.RestPost
import org.mashupbots.socko.rest.RestPut
import org.mashupbots.socko.rest.RestQuery
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

class UserDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

@RestPost(urlTemplate = "/user.json/createWithArray", dispatcherClass = "UserDispatcher")
case class CreateUserWithArrayRequest(context: RestRequestContext, @RestBody() users: Array[User]) extends RestRequest
case class CreateUserWithArrayResponse(context: RestResponseContext) extends RestResponse

@RestPost(urlTemplate = "/user.json", dispatcherClass = "UserDispatcher")
case class CreateUserRequest(context: RestRequestContext, @RestBody() user: User) extends RestRequest
case class CreateUserResponse(context: RestResponseContext) extends RestResponse

@RestPost(urlTemplate = "/user.json/createWithList", dispatcherClass = "UserDispatcher")
case class CreateUserWithListRequest(context: RestRequestContext, @RestBody() users: Seq[User]) extends RestRequest
case class CreateUserWithListResponse(context: RestResponseContext) extends RestResponse

@RestPut(urlTemplate = "/user.json/{username}", dispatcherClass = "UserDispatcher")
case class UpdateUserRequest(context: RestRequestContext, @RestPath() username: String, @RestBody() user: User) extends RestRequest
case class UpdateUserResponse(context: RestResponseContext) extends RestResponse

@RestDelete(urlTemplate = "/user.json/{username}", dispatcherClass = "UserDispatcher")
case class DeleteUserRequest(context: RestRequestContext, @RestPath() username: String) extends RestRequest
case class DeleteUserResponse(context: RestResponseContext) extends RestResponse

@RestGet(urlTemplate = "/user.json/{username}", dispatcherClass = "UserDispatcher")
case class GetUserRequest(context: RestRequestContext, @RestPath() username: String) extends RestRequest
case class GetUserResponse(context: RestResponseContext, user: Option[User]) extends RestResponse

@RestGet(urlTemplate = "/user.json/login", dispatcherClass = "UserDispatcher")
case class LoginRequest(context: RestRequestContext, @RestQuery() username: String, @RestQuery() password: String) extends RestRequest
case class LoginResponse(context: RestResponseContext, result: String) extends RestResponse

@RestGet(urlTemplate = "/user.json/logout", dispatcherClass = "UserDispatcher")
case class LogoutRequest(context: RestRequestContext) extends RestRequest
case class LogoutResponse(context: RestResponseContext) extends RestResponse


