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
package org.mashupbots.socko.rest.petshop

import java.util.Date

import org.mashupbots.socko.rest.BodyParam
import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.RestDeclaration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.ActorRef
import akka.actor.ActorSystem

case class Order(
  id: Long,
  petId: Long,
  status: String,
  quantity: Int,
  shipDate: Date)

case class GetOrderRequest(context: RestRequestContext, orderId: String) extends RestRequest
case class GetOrderResponse(context: RestResponseContext, order: Option[Order]) extends RestResponse
object GetOrderDeclaration extends RestDeclaration {
  val method = Method.GET
  val path = "/store/order/{orderId}"
  val requestParams = Seq(PathParam("orderId"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class DeleteOrderRequest(context: RestRequestContext, orderId: String) extends RestRequest
case class DeleteOrderResponse(context: RestResponseContext) extends RestResponse
object DeleteOrderDeclaration extends RestDeclaration {
  val method = Method.DELETE
  val path = "/store/order/{orderId}"
  val requestParams = Seq(PathParam("orderId"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class PlaceOrderRequest(context: RestRequestContext, order: Order) extends RestRequest
case class PlaceOrderResponse(context: RestResponseContext) extends RestResponse
object PostOrderDeclaration extends RestDeclaration {
  val method = Method.POST
  val path = "/store/order"
  val requestParams = Seq(BodyParam("order"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}


