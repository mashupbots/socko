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
package org.mashupbots.socko.rest.put

import org.mashupbots.socko.rest.BodyParam
import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.RestRegistration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object PutBytesRegistration extends RestRegistration {
  val method = Method.PUT
  val path = "/bytes/{status}"
  val requestParams = Seq(PathParam("status"), BodyParam("bytes"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[PutBytesProcessor])
}

case class PutBytesRequest(context: RestRequestContext, status: Int, bytes: Seq[Byte]) extends RestRequest

case class PutBytesResponse(context: RestResponseContext, data: Seq[Byte]) extends RestResponse

class PutBytesProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PutBytesRequest =>
      if (req.status == 200) {
        sender ! PutBytesResponse(req.context.responseContext(req.status, Map.empty), req.bytes)
      } else {
        sender ! PutBytesResponse(req.context.responseContext(req.status), Seq.empty)
      }
      context.stop(self)
  }
}
