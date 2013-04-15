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

import java.util.Date

import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.RestDeclaration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object GetWithErrorsDeclaration extends RestDeclaration {
  val method = Method.GET
  val path = "/error/{error}"
  val requestParams = Seq(PathParam("error"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[GetWithErrorsProcessor])
}

case class GetWithErrorsRequest(context: RestRequestContext, error: String) extends RestRequest

case class GetWithErrorsResponse(context: RestResponseContext, data: Option[Date]) extends RestResponse

class GetWithErrorsProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: GetWithErrorsRequest =>
      req.error match {
        case "exception" => throw new IllegalStateException("Generated exception")
        case "timeout" => log.debug("do nothing so we timeout")
      }
      context.stop(self)
  }
}
