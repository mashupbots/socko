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
import org.mashupbots.socko.rest.RestPost
import org.mashupbots.socko.rest.RestPath
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import java.util.Date

@RestPost(urlTemplate = "/object/{status}")
case class PostObjectRequest(context: RestRequestContext, @RestPath() status: Int, @RestBody() fish: Fish) extends RestRequest

case class Event(date: Date, description: String)
case class Fish(name: String, age: Int, history: List[Event])

case class PostObjectResponse(context: RestResponseContext, pet: Fish) extends RestResponse

class PostObjectProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PostObjectRequest =>
      if (req.status == 200) {
        sender ! PostObjectResponse(
          req.context.responseContext(HttpResponseStatus(req.status)),
          req.fish)
      } else {
        sender ! PostObjectResponse(
          req.context.responseContext(HttpResponseStatus(req.status)),
          null)
      }
      context.stop(self)
  }
}

class PostObjectDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = {
    actorSystem.actorOf(Props[PostObjectProcessor])
  }
}
