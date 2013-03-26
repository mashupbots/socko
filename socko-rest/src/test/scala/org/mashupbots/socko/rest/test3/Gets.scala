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
package org.mashupbots.socko.rest.test3

import org.mashupbots.socko.rest.RestGet
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext
import org.mashupbots.socko.rest.RestPath
import akka.actor.ActorRef
import akka.actor.ActorSystem
import org.mashupbots.socko.rest.RestDispatcher

@RestGet(urlTemplate = "/pets/{id}")
case class GetPetRequest(context: RestRequestContext, 
    @RestPath() id: String) extends RestRequest 

case class GetPetResponse(context: RestResponseContext) extends RestResponse 

class GetPetDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}
