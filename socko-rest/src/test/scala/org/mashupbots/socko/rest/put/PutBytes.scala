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

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.rest.RestBody
import org.mashupbots.socko.rest.RestDispatcher
import org.mashupbots.socko.rest.RestPut
import org.mashupbots.socko.rest.RestPath
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

@RestPut(urlTemplate = "/bytes/{status}")
case class PutBytesRequest(context: RestRequestContext, @RestPath() status: Int, @RestBody() bytes: Seq[Byte]) extends RestRequest

case class PutBytesResponse(context: RestResponseContext, data: Seq[Byte]) extends RestResponse

class PutBytesProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PutBytesRequest =>
      if (req.status == 200) {
        sender ! PutBytesResponse(req.context.responseContext(HttpResponseStatus(req.status), Map.empty), req.bytes)
      } else {
        sender ! PutBytesResponse(req.context.responseContext(HttpResponseStatus(req.status)), Seq.empty)
      }
      context.stop(self)
  }
}

class PutBytesDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = {
    actorSystem.actorOf(Props[PutBytesProcessor])
  }
}
