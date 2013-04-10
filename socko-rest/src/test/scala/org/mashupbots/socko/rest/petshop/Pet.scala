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

@RestGet(urlTemplate = "/pet.json/{petId}", dispatcherClass = "PetDispatcher")
case class GetPetRequest(context: RestRequestContext, @RestPath() petId: String) extends RestRequest
case class GetPetResponse(context: RestResponseContext, pet: Option[Pet]) extends RestResponse

@RestPost(urlTemplate = "/pet.json", dispatcherClass = "PetDispatcher")
case class CreatePetRequest(context: RestRequestContext, @RestBody() pet: Pet) extends RestRequest
case class CreatePetResponse(context: RestResponseContext) extends RestResponse

@RestPut(urlTemplate = "/pet.json", dispatcherClass = "PetDispatcher")
case class UpdatePetRequest(context: RestRequestContext, @RestBody() pet: Pet) extends RestRequest
case class UpdatePetResponse(context: RestResponseContext) extends RestResponse

@RestPut(urlTemplate = "/pet.json.findByStatus", dispatcherClass = "PetDispatcher")
case class FindPetByStatusRequest(context: RestRequestContext, @RestQuery() status: String) extends RestRequest
case class FindPetByStatusResponse(context: RestResponseContext, pet: Seq[Pet]) extends RestResponse

@RestPut(urlTemplate = "/pet.json.findByTags", dispatcherClass = "PetDispatcher")
case class FindPetByTagsRequest(context: RestRequestContext, @RestQuery() tags: String) extends RestRequest
case class FindPetByTagsResponse(context: RestResponseContext, pet: Seq[Pet]) extends RestResponse

