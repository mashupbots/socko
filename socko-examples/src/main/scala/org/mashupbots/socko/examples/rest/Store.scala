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
package org.mashupbots.socko.examples.rest

import java.util.Date

import scala.collection.mutable.ListBuffer

import org.mashupbots.socko.rest.AllowableValuesList
import org.mashupbots.socko.rest.BodyParam
import org.mashupbots.socko.rest.Error
import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.RestModelMetaData
import org.mashupbots.socko.rest.RestPropertyMetaData
import org.mashupbots.socko.rest.RestRegistration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala

//*************************************************************
// Model
//*************************************************************
case class Order(
  id: Long,
  petId: Long,
  status: String,
  quantity: Int,
  shipDate: Date)
object Order extends RestModelMetaData {
  val modelProperties = Seq(
    RestPropertyMetaData("status", "Order Status", Some(AllowableValuesList(List("placed", " approved", " delivered")))))
}

//*************************************************************
// Data
//*************************************************************
object StoreData {
  val orders: ListBuffer[Order] = new ListBuffer[Order]()

  orders += createOrder(1, 1, 2, new Date(), "placed")
  orders += createOrder(2, 1, 2, new Date(), "delivered")
  orders += createOrder(3, 2, 2, new Date(), "placed")
  orders += createOrder(4, 2, 2, new Date(), "delivered")
  orders += createOrder(5, 3, 2, new Date(), "placed")

  def findOrderById(orderId: Long): Order = {
    for (order <- orders) {
      if (order.id == orderId) {
        return order
      }
    }
    null
  }

  def placeOrder(order: Order): Unit = {
    // remove any pets with same id
    orders --= orders.filter(o => o.id == order.id)
    orders += order
  }

  def deleteOrder(orderId: Long): Unit = {
    orders --= orders.filter(o => o.id == orderId)
  }

  private def createOrder(id: Long, petId: Long, quantity: Int, shipDate: Date, status: String): Order = {
    val order = Order(id, petId, status, quantity, shipDate)
    order
  }
}

//*************************************************************
// API
//*************************************************************
case class GetOrderRequest(context: RestRequestContext, orderId: Long) extends RestRequest
case class GetOrderResponse(context: RestResponseContext, order: Option[Order]) extends RestResponse
class GetOrderProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: GetOrderRequest =>
      val order = StoreData.findOrderById(req.orderId)
      val response = if (order != null) {
        GetOrderResponse(req.context.responseContext, Some(order))
      } else {
        GetOrderResponse(req.context.responseContext(404), None)
      }
      sender ! response
      context.stop(self)
  }
}
object GetOrderRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/store/order/{orderId}"
  val requestParams = Seq(PathParam("orderId", "ID of pet that needs to be fetched"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[GetOrderProcessor])
  override val description = "Find purchase order by ID"
  override val notes = "For valid response try integer IDs with value <= 5. Nonintegers will generate API errors"
  override val errors = Seq(Error(400, "Invalid ID supplied"), Error(404, "Order not found"))
}

case class DeleteOrderRequest(context: RestRequestContext, orderId: Long) extends RestRequest
case class DeleteOrderResponse(context: RestResponseContext) extends RestResponse
class DeleteOrderProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: DeleteOrderRequest =>
      StoreData.deleteOrder(req.orderId)
      sender ! DeleteOrderResponse(req.context.responseContext)
      context.stop(self)
  }
}
object DeleteOrderRegistration extends RestRegistration {
  val method = Method.DELETE
  val path = "/store/order/{orderId}"
  val requestParams = Seq(PathParam("orderId", "ID of the order that needs to be deleted"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[DeleteOrderProcessor])
  override val description = "Delete purchase order by ID"
  override val notes = "For valid response try integer IDs with value < 5. Nonintegers will generate API errors"
  override val errors = Seq(Error(400, "Invalid ID supplied"), Error(404, "Order not found"))
}

case class PlaceOrderRequest(context: RestRequestContext, order: Order) extends RestRequest
case class PlaceOrderResponse(context: RestResponseContext) extends RestResponse
class PlaceOrderProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PlaceOrderRequest =>
      StoreData.placeOrder(req.order)
      sender ! PlaceOrderResponse(req.context.responseContext)
      context.stop(self)
  }
}
object PlaceOrderRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/store/order"
  val requestParams = Seq(BodyParam("order", "order placed for purchasing the pet"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[PlaceOrderProcessor])
  override val description = "Place an order for a pet"
  override val errors = Seq(Error(400, "Invalid order"))
}
