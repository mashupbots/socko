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
package org.mashupbots.socko.examples.rest

import scala.collection.mutable.ListBuffer

import org.mashupbots.socko.rest.AllowableValuesList
import org.mashupbots.socko.rest.BodyParam
import org.mashupbots.socko.rest.Error
import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.QueryParam
import org.mashupbots.socko.rest.RestModelMetaData
import org.mashupbots.socko.rest.RestPropertyMetaData
import org.mashupbots.socko.rest.RestRegistration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.actorRef2Scala

//*************************************************************
// Model
//*************************************************************
case class User(
  id: Long,
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  password: String,
  phone: String,
  userStatus: Int)
object User extends RestModelMetaData {
  val modelProperties = Seq(
    RestPropertyMetaData("userStatus", "User status", Some(AllowableValuesList(List("1-registered", "2-active", "3-closed")))))
}

//*************************************************************
// Data
//*************************************************************
object UserData {
  val users: ListBuffer[User] = new ListBuffer[User]()

  users += createUser(1, "user1", "first name 1", "last name 1", "email1@test.com", "123-456-7890", 1)
  users += createUser(2, "user2", "first name 2", "last name 2", "email2@test.com", "123-456-7890", 2)
  users += createUser(3, "user3", "first name 3", "last name 3", "email3@test.com", "123-456-7890", 3)
  users += createUser(4, "user4", "first name 4", "last name 4", "email4@test.com", "123-456-7890", 1)
  users += createUser(5, "user5", "first name 5", "last name 5", "email5@test.com", "123-456-7890", 2)
  users += createUser(6, "user6", "first name 6", "last name 6", "email6@test.com", "123-456-7890", 3)
  users += createUser(7, "user7", "first name 7", "last name 7", "email7@test.com", "123-456-7890", 1)
  users += createUser(8, "user8", "first name 8", "last name 8", "email8@test.com", "123-456-7890", 2)
  users += createUser(9, "user9", "first name 9", "last name 9", "email9@test.com", "123-456-7890", 3)
  users += createUser(10, "user10", "first name 10", "last name 10", "email10@test.com", "123-456-7890", 1)
  users += createUser(11, "user?10", "first name ?10", "last name ?10", "email101@test.com", "123-456-7890", 1)

  def findUserByName(username: String): User = {
    for (user <- users) {
      if (user.username == username) {
        return user
      }
    }
    null
  }

  def addUser(user: User): Unit = {
    users --= users.filter(u => u.id == user.id)
    users += user
  }

  def removeUser(username: String): Unit = {
    for (user <- users) {
      if (user.username == username) {
        users -= user
      }
    }
  }

  private def createUser(id: Long, username: String, firstName: String, lastName: String, email: String, phone: String, userStatus: Int): User = {
    var user = User(id, username, firstName, lastName, email, "XXXXXXXXXXX", phone, userStatus)
    user
  }
}

//*************************************************************
// API
//*************************************************************
case class CreateUserWithArrayRequest(context: RestRequestContext, users: Array[User]) extends RestRequest
case class CreateUserWithArrayResponse(context: RestResponseContext) extends RestResponse
object CreateUserWithArrayRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/user/createWithArray"
  val requestParams = Seq(BodyParam("users"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val description = "Creates list of users with given input array"
}

case class CreateUserRequest(context: RestRequestContext, user: User) extends RestRequest
case class CreateUserResponse(context: RestResponseContext) extends RestResponse
object CreateUserRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/user"
  val requestParams = Seq(BodyParam("user"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val description = "Create user"
  override val notes = "This can only be done by the logged in user."
}

case class CreateUserWithListRequest(context: RestRequestContext, users: Seq[User]) extends RestRequest
case class CreateUserWithListResponse(context: RestResponseContext) extends RestResponse
object CreateUserWithListRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/user/createWithList"
  val requestParams = Seq(BodyParam("users"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val description = "Creates list of users with given list inpu"
}

case class UpdateUserRequest(context: RestRequestContext, username: String, user: User) extends RestRequest
case class UpdateUserResponse(context: RestResponseContext) extends RestResponse
object UpdateUserRegistration extends RestRegistration {
  val method = Method.PUT
  val path = "/user/{username}"
  val requestParams = Seq(PathParam("username"), BodyParam("user"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val description = "Update user"
  override val notes = "This can only be done by the logged in user."
  override val errors = Seq(Error(400, "Invalid username supplied"), Error(404, "User not found"))
}

case class DeleteUserRequest(context: RestRequestContext, username: String) extends RestRequest
case class DeleteUserResponse(context: RestResponseContext) extends RestResponse
object DeleteUserRegistration extends RestRegistration {
  val method = Method.DELETE
  val path = "/user/{username}"
  val requestParams = Seq(PathParam("username"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val description = "Delete user"
  override val notes = "This can only be done by the logged in user."
  override val errors = Seq(Error(400, "Invalid username supplied"), Error(404, "User not found"))
}

case class GetUserRequest(context: RestRequestContext, username: String) extends RestRequest
case class GetUserResponse(context: RestResponseContext, user: Option[User]) extends RestResponse
object GetUserRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/user/{username}"
  val requestParams = Seq(PathParam("username"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val description = "Get user by user name"
  override val errors = Seq(Error(400, "Invalid username supplied"), Error(404, "User not found"))
}

case class LoginRequest(context: RestRequestContext, username: String, password: String) extends RestRequest
case class LoginResponse(context: RestResponseContext, result: String) extends RestResponse
object LoginRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/user/login"
  val requestParams = Seq(QueryParam("username"), QueryParam("password"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val errors = Seq(Error(400, "Invalid username and password combination"))
}

case class LogoutRequest(context: RestRequestContext) extends RestRequest
case class LogoutResponse(context: RestResponseContext) extends RestResponse
object LogoutRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/user/logout"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorFor("/user/user-api-router")
  override val description = "Find purchase order by ID"
  override val notes = "For valid response try integer IDs with value <= 5. Nonintegers will generate API errors"
  override val errors = Seq(Error(400, "Invalid ID supplied"), Error(404, "Order not found"))
}

/**
 * Example illustrating using a single Actor to process all user requests.
 * This actor will be setup to run under an akka router.
 */
class UserProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: CreateUserWithArrayRequest =>
      for (user <- req.users) {
        UserData.addUser(user)
      }
      sender ! CreateUserWithArrayResponse(req.context.responseContext)
    case req: CreateUserRequest =>
      UserData.addUser(req.user)
      sender ! CreateUserResponse(req.context.responseContext)
    case req: CreateUserWithListRequest =>
      for (user <- req.users) {
        UserData.addUser(user)
      }
      sender ! CreateUserWithListResponse(req.context.responseContext)
    case req: UpdateUserRequest =>
      UserData.addUser(req.user)
      sender ! UpdatePetResponse(req.context.responseContext)
    case req: DeleteUserRequest =>
      UserData.removeUser(req.username)
      sender ! DeleteUserResponse(req.context.responseContext)
    case req: GetUserRequest =>
      val user = UserData.findUserByName(req.username)
      val response = if (user != null) {
        GetUserResponse(req.context.responseContext, Some(user))
      } else {
        GetUserResponse(req.context.responseContext(404), None)
      }
      sender ! response
    case req: LoginRequest =>
      sender ! LoginResponse(req.context.responseContext, "logged in user session:" + System.currentTimeMillis())
    case req: LogoutRequest =>
      sender ! LogoutResponse(req.context.responseContext)
  }
}

