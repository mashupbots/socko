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

import org.mashupbots.socko.rest.AllowableValues
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
import akka.actor.ActorRef
import akka.actor.ActorSystem
import scala.collection.mutable.ListBuffer
import akka.actor.Actor
import akka.actor.Props

//*************************************************************
// Model
//*************************************************************
case class Tag(id: Long, name: String)
case class Category(id: Long, name: String)
case class Pet(id: Long, category: Category, name: String, photoUrls: List[String], tags: List[Tag], status: String)
object Pet extends RestModelMetaData {
  val modelProperties = Seq(
    RestPropertyMetaData("status", "pet status in the store", Some(AllowableValuesList(List("available", "pending", "sold")))))
}

//*************************************************************
// Data
//*************************************************************
object PetData {
  val pets: ListBuffer[Pet] = new ListBuffer[Pet]()
  val categories: ListBuffer[Category] = new ListBuffer[Category]()

  categories ++= List(
    Category(1, "Dogs"),
    Category(2, "Cats"),
    Category(3, "Rabbits"),
    Category(4, "Lions"))

  pets ++= List(
    Pet(1, categories(1), "Cat 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(2, categories(1), "Cat 2", List("url1", "url2"), List(Tag(2, "tag2"), Tag(3, "tag3")), "available"),
    Pet(3, categories(1), "Cat 3", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "pending"),
    Pet(4, categories(0), "Dog 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(5, categories(0), "Dog 2", List("url1", "url2"), List(Tag(2, "tag2"), Tag(3, "tag3")), "sold"),
    Pet(6, categories(0), "Dog 3", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "pending"),
    Pet(7, categories(3), "Lion 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(8, categories(3), "Lion 2", List("url1", "url2"), List(Tag(2, "tag2"), Tag(3, "tag3")), "available"),
    Pet(9, categories(3), "Lion 3", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "available"),
    Pet(10, categories(2), "Rabbit 1", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "available"))

  def getPetById(petId: Long): Pet = {
    val pet = pets.filter(p => p.id == petId).toList
    pet.size match {
      case 0 => null
      case _ => pet.head
    }
  }

  def findPetByStatus(status: String): List[Pet] = {
    var statues = status.split(",")
    var result = new ListBuffer[Pet]()
    for (pet <- pets) {
      if (statues.contains(pet.status)) {
        result += pet
      }
    }
    result.toList
  }

  def findPetByTags(tags: String): List[Pet] = {
    var tagList = tags.split(",")
    var result = new ListBuffer[Pet]()
    for (pet <- pets) {
      if (pet.tags != null)
        for (tag <- pet.tags) {
          if (tagList.contains(tag.name)) {
            result += pet
          }
        }
    }
    result.toList
  }

  def addPet(pet: Pet): Unit = {
    // remove any pets with same id
    pets --= pets.filter(p => p.id == pet.id)
    pets += pet
  }
}

//*************************************************************
// API
//*************************************************************
case class GetPetRequest(context: RestRequestContext, petId: Long) extends RestRequest
case class GetPetResponse(context: RestResponseContext, pet: Option[Pet]) extends RestResponse
object GetPetRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pet/{petId}"
  val requestParams = Seq(PathParam("petId", "ID of pet that needs to be fetched"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = actorSystem.actorOf(Props[PetProcessor])
  override val name = "getPetById"
  override val description = "Find pet by ID"
  override val notes = "Returns a pet based on ID"
  override val errors = Seq(Error(400, "Invalid ID supplied"), Error(404, "Pet not found"))
}

case class CreatePetRequest(context: RestRequestContext, pet: Pet) extends RestRequest
case class CreatePetResponse(context: RestResponseContext) extends RestResponse
object CreatePetRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/pet"
  val requestParams = Seq(BodyParam("pet", "Pet object that needs to be added to the store"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = actorSystem.actorOf(Props[PetProcessor])
  override val name = "addPet"
  override val description = "Add a new pet to the store"
  override val errors = Seq(Error(405, "Invalid input"))
}

case class UpdatePetRequest(context: RestRequestContext, pet: Pet) extends RestRequest
case class UpdatePetResponse(context: RestResponseContext) extends RestResponse
object UpdatePetRegistration extends RestRegistration {
  val method = Method.PUT
  val path = "/pet"
  val requestParams = Seq(BodyParam("pet", "Pet object that needs to be updated in the store"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = actorSystem.actorOf(Props[PetProcessor])
  override val name = "updatePet"
  override val description = "Update an existing pet"
  override val errors = Seq(Error(400, "Invalid ID supplied"), Error(404, "Pet not found"), Error(405, "Validation exception"))
}

case class FindPetByStatusRequest(context: RestRequestContext, status: String) extends RestRequest
case class FindPetByStatusResponse(context: RestResponseContext, pet: Seq[Pet]) extends RestResponse
object FindPetByStatusRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pet/findByStatus"
  val requestParams = Seq(
    QueryParam("status", "Status values that need to be considered for filter", allowMultiple = true,
      allowableValues = Some(AllowableValues(List("available", "pending", "sold")))))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = actorSystem.actorOf(Props[PetProcessor])
  override val name = "findPetsByStatus"
  override val description = "Finds Pets by status"
  override val notes = "Multiple status values can be provided with comma seperated strings"
  override val errors = Seq(Error(405, "Invalid status value"))
}

case class FindPetByTagsRequest(context: RestRequestContext, tags: String) extends RestRequest
case class FindPetByTagsResponse(context: RestResponseContext, pet: Seq[Pet]) extends RestResponse
object FindPetByTagsRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pet/findPetsByTags"
  val requestParams = Seq(QueryParam("tags", "Tags to filter by"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = actorSystem.actorOf(Props[PetProcessor])
  override val name = "findPetsByTags"
  override val description = "Finds Pets by tags"
  override val notes = "Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing."
  override val errors = Seq(Error(405, "Invalid tag value"))
  override val deprecated = true
}

/**
 * Example illustrating using a single Actor to process all PET requests.
 * A new actor instance is created for each request by the `processorActor()` method.
 * The instance is terminates itself when processing is finished. 
 */
class PetProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: GetPetRequest =>
      val pet = PetData.getPetById(req.petId)
      val response = if (pet != null) {
        GetPetResponse(req.context.responseContext, Some(pet))
      } else {
        GetPetResponse(req.context.responseContext(404), None)
      }
      sender ! response
      context.stop(self)
    case req: CreatePetRequest =>
      PetData.addPet(req.pet)
      val response = CreatePetResponse(req.context.responseContext)
      sender ! response
      context.stop(self)
    case req: UpdatePetRequest =>
      PetData.addPet(req.pet)
      val response = UpdatePetResponse(req.context.responseContext)
      sender ! response
      context.stop(self)
    case req: FindPetByStatusRequest =>
      val pets = PetData.findPetByStatus(req.status)
      val response = FindPetByStatusResponse(req.context.responseContext, pets)
      sender ! response
      context.stop(self)
    case req: FindPetByTagsRequest =>
      val pets = PetData.findPetByTags(req.tags)
      val response = FindPetByTagsResponse(req.context.responseContext, pets)
      sender ! response
      context.stop(self)
  }
}
