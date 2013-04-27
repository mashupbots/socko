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

import org.mashupbots.socko.rest.AllowableValuesList
import org.mashupbots.socko.rest.BodyParam
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
import akka.actor.ActorRef
import akka.actor.ActorSystem
import scala.collection.mutable.ListBuffer

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
    RestPropertyMetaData("status", "User status", Some(AllowableValuesList(List("1-registered", "2-active", "3-closed")))))
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


