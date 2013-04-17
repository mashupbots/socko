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

case class Tag(id: Long, name: String)
case class Category(id: Long, name: String)
case class Pet(tags: Seq[Tag], id: Long, category: Category, status: String, name: String, photoUrls: Seq[String])

case class GetPetRequest(context: RestRequestContext, petId: String) extends RestRequest
case class GetPetResponse(context: RestResponseContext, pet: Option[Pet]) extends RestResponse
object GetPetRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pet/{petId}"
  val requestParams = Seq(PathParam("petId", "ID of pet that needs to be fetched"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
  override val name = "getPetById"
  override val description = "Find pet by ID"
  override val notes = "Returns a pet based on ID"
  override val errors = Seq(Error(400, "Invalid ID supplied"), Error(401, "Pet not found"))
}

case class CreatePetRequest(context: RestRequestContext, pet: Pet) extends RestRequest
case class CreatePetResponse(context: RestResponseContext) extends RestResponse
object CreatePetRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/pet"
  val requestParams = Seq(BodyParam("pet", "Pet object that needs to be added to the store"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
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
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
  override val name = "updatePet"
  override val description = "Update an existing pet"
  override val errors = Seq(Error(400, "Invalid ID supplied"), Error(404, "Pet not found"), Error(405, "Validation exception"))
}

case class FindPetByStatusRequest(context: RestRequestContext, status: String) extends RestRequest
case class FindPetByStatusResponse(context: RestResponseContext, pet: Seq[Pet]) extends RestResponse
object FindPetByStatusRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pet/findByStatus"
  val requestParams = Seq(QueryParam("status", "Status values that need to be considered for filter"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
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
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
  override val name = "findPetsByTags"
  override val description = "Finds Pets by tags"
  override val notes = "Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing."
  override val errors = Seq(Error(405, "Invalid tag value"))
  override val deprecated = true
}
