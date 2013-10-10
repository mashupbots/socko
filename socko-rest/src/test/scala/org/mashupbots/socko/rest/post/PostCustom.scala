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

import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.RestRegistration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestRequestEvents

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object PostCustomRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/custom"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[PostCustomProcessor])
  override val customDeserialization = true
  override val customSerialization = true
}

case class PostCustomRequest(context: RestRequestContext) extends RestRequest

// Note that response is not required because `customSerialization = true`

class PostCustomProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PostCustomRequest =>
      val http = RestRequestEvents.get(req.context).get.asInstanceOf[HttpRequestEvent]
      http.response.write(http.request.content.toString())
      context.stop(self)
  }
}
