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

case class Tag(id: Long, name: String)
case class Category(id: Long, name: String)
case class Pet(tags: Seq[Tag], id: Long, category: Category, status: String, name: String, photoUrls: Seq[String])

class PetDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

@RestGet(path = "/pet/{petId}",
  dispatcherClass = "PetDispatcher",
  name = "getPetById",
  description = "Find pet by ID",
  notes = "Returns a pet based on ID")
case class GetPetRequest(context: RestRequestContext,
  @RestPath(description = "ID of pet that needs to be fetched") petId: String) extends RestRequest
case class GetPetResponse(context: RestResponseContext, pet: Option[Pet]) extends RestResponse

@RestPost(path = "/pet",
  dispatcherClass = "PetDispatcher",
  name = "addPet",
  description = "Add a new pet to the store")
case class CreatePetRequest(context: RestRequestContext,
  @RestBody(description = "Pet object that needs to be added to the store") pet: Pet) extends RestRequest
case class CreatePetResponse(context: RestResponseContext) extends RestResponse

@RestPut(path = "/pet",
  dispatcherClass = "PetDispatcher",
  name = "updatePet",
  description = "Update an existing pet")
case class UpdatePetRequest(context: RestRequestContext,
  @RestBody(description = "Pet object that needs to be updated in the store") pet: Pet) extends RestRequest
case class UpdatePetResponse(context: RestResponseContext) extends RestResponse

@RestPut(path = "/pet/findByStatus",
  dispatcherClass = "PetDispatcher",
  name = "findPetsByStatus",
  description = "Finds Pets by status",
  notes = "Multiple status values can be provided with comma seperated strings")
case class FindPetByStatusRequest(context: RestRequestContext,
  @RestQuery(description = "Status values that need to be considered for filter") status: String) extends RestRequest
case class FindPetByStatusResponse(context: RestResponseContext, pet: Seq[Pet]) extends RestResponse

@RestPut(path = "/pet/findByTags",
  dispatcherClass = "PetDispatcher",
  name = "findPetsByTags",
  description = "Finds Pets by tags",
  notes = "Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing.")
case class FindPetByTagsRequest(context: RestRequestContext, 
    @RestQuery(description = "Tags to filter by") tags: String) extends RestRequest
case class FindPetByTagsResponse(context: RestResponseContext, pet: Seq[Pet]) extends RestResponse

