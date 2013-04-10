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

import org.mashupbots.socko.rest.RestBody
import org.mashupbots.socko.rest.RestDelete
import org.mashupbots.socko.rest.RestDispatcher
import org.mashupbots.socko.rest.RestGet
import org.mashupbots.socko.rest.RestPath
import org.mashupbots.socko.rest.RestPost
import org.mashupbots.socko.rest.RestPut
import org.mashupbots.socko.rest.RestQuery
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext
import akka.actor.ActorRef
import akka.actor.ActorSystem
import java.util.Date

case class Order(
  id: Long,
  petId: Long,
  status: String,
  quantity: Int,
  shipDate: Date)

class StoreDispatcher extends RestDispatcher {
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

@RestGet(urlTemplate = "/store.json/order/{orderId}", dispatcherClass = "UserDispatcher")
case class GetOrderRequest(context: RestRequestContext, @RestPath() orderId: String) extends RestRequest
case class GetOrderResponse(context: RestResponseContext, order: Option[Order]) extends RestResponse

@RestDelete(urlTemplate = "/store.json/order/{orderId}", dispatcherClass = "StoreDispatcher")
case class DeleteOrderRequest(context: RestRequestContext, @RestPath() orderId: String) extends RestRequest
case class DeleteOrderResponse(context: RestResponseContext) extends RestResponse

@RestPost(urlTemplate = "/store.json/order", dispatcherClass = "StoreDispatcher")
case class PlaceOrderRequest(context: RestRequestContext, @RestBody() order: Order) extends RestRequest
case class PlaceOrderResponse(context: RestResponseContext) extends RestResponse    


