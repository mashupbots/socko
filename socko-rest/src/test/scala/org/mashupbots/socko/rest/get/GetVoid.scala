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
import org.mashupbots.socko.rest.RestProcessorLocator

@RestGet(urlTemplate = "/void/{status}")
case class GetVoidRequest(context: RestRequestContext, @RestPath() status: Int) extends RestRequest {

  def processingActor(actorSystem: ActorSystem): ActorRef = {
    actorSystem.actorOf(Props[GetVoidProcessor])
  }
}

case class GetVoidResponse(context: RestResponseContext) extends RestResponse {

}

class GetVoidProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: GetVoidRequest =>
      sender ! GetVoidResponse(req.context.responseContext(HttpResponseStatus(req.status)))
      context.stop(self)
  }
}

class GetVoidProcessorLocator extends RestProcessorLocator {
  def locateProcessor(actorSystem: ActorSystem): ActorRef = {
    actorSystem.actorOf(Props[GetVoidProcessor])
  }  
}