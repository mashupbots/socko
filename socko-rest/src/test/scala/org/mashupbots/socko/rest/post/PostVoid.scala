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
package org.mashupbots.socko.rest.post

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.rest.RestBody
import org.mashupbots.socko.rest.RestDispatcher
import org.mashupbots.socko.rest.RestPath
import org.mashupbots.socko.rest.RestPost
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

@RestPost(urlTemplate = "/void/{status}")
case class PostVoidRequest(context: RestRequestContext, @RestPath() status: Int, @RestBody() pet: Pet) extends RestRequest

case class PostVoidResponse(context: RestResponseContext) extends RestResponse

class PostVoidProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PostVoidRequest =>
      sender ! PostVoidResponse(req.context.responseContext(HttpResponseStatus(req.status)))
      context.stop(self)
  }
}

class PostVoidDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = {
    actorSystem.actorOf(Props[PostVoidProcessor])
  }
}

case class Pet(name: String, age: Int)

