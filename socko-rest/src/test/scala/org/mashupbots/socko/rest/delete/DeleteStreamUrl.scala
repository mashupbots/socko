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
package org.mashupbots.socko.rest.delete

import java.net.URL

import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.QueryParam
import org.mashupbots.socko.rest.RestDeclaration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object DeleteStreamDeclaration extends RestDeclaration {
  val method = Method.DELETE
  val path = "/streamurl/{status}"
  val requestParams = Seq(PathParam("status"), QueryParam("sourceURL"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[DeleteStreamUrlProcessor])
}

case class DeleteStreamUrlRequest(context: RestRequestContext,
  status: Int,
  sourceURL: String) extends RestRequest

case class DeleteStreamUrlResponse(context: RestResponseContext, data: URL) extends RestResponse

class DeleteStreamUrlProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: DeleteStreamUrlRequest =>
      if (req.status == 200) {
        sender ! DeleteStreamUrlResponse(
          req.context.responseContext(req.status, Map("Content-Type" -> "text/plain; charset=UTF-8")),
          new URL(req.sourceURL))
      } else {
        sender ! DeleteStreamUrlResponse(
          req.context.responseContext(req.status), null)
      }
      context.stop(self)
  }
}
