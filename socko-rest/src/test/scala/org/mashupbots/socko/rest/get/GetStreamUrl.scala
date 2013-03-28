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
package org.mashupbots.socko.rest.get

import org.mashupbots.socko.rest.RestGet
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext
import akka.actor.Actor
import org.mashupbots.socko.rest.RestPath
import org.mashupbots.socko.events.HttpResponseStatus
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import org.mashupbots.socko.rest.RestDispatcher
import org.mashupbots.socko.infrastructure.CharsetUtil
import java.net.URL
import org.mashupbots.socko.rest.RestQuery

@RestGet(urlTemplate = "/streamurl/{status}")
case class GetStreamUrlRequest(context: RestRequestContext,
  @RestPath() status: Int,
  @RestQuery() sourceURL: String) extends RestRequest

case class GetStreamUrlResponse(context: RestResponseContext, data: URL) extends RestResponse

class GetStreamUrlProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: GetStreamUrlRequest =>
      if (req.status == 200) {
        sender ! GetStreamUrlResponse(
          req.context.responseContext(HttpResponseStatus(req.status), Map("Content-Type" -> "text/plain; charset=UTF-8")),
          new URL(req.sourceURL))
      } else {
        sender ! GetStreamUrlResponse(
          req.context.responseContext(HttpResponseStatus(req.status)), null)
      }
      context.stop(self)
  }
}

class GetStreamUrlDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = {
    actorSystem.actorOf(Props[GetStreamUrlProcessor])
  }
}
