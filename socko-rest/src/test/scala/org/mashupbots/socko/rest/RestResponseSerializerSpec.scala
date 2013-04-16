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
package org.mashupbots.socko.rest

import java.net.URL
import java.util.Date
import scala.reflect.runtime.{ universe => ru }
import org.mashupbots.socko.events.EndPoint
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import akka.actor.ActorSystem
import akka.actor.ActorRef

class RestResponseSerializerSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestResponseSerializerSpec" must {

    val config = RestConfig("1.0", "/api")
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val requestContext = RestRequestContext(EndPoint("GET", "localhost", "/path/1234"), Map.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)
    val responseContext = RestResponseContext(requestContext, HttpResponseStatus(200), Map.empty)

    "Serailize void response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[VoidResponse].typeSymbol.asClass)

      s.responseDataType must be(ResponseDataType.Void)
      s.responseDataTerm must be(None)
    }

    "Serailize primitive string response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[StringResponse].typeSymbol.asClass)

      s.responseDataType must be(ResponseDataType.Primitive)

      val response = StringResponse(responseContext, "hello")
      val x = s.getData(response)

      x.asInstanceOf[String] must be("hello")
    }

    "Serailize primitive optional int response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[OptionalIntResponse].typeSymbol.asClass)

      s.responseDataType must be(ResponseDataType.Primitive)

      val response = OptionalIntResponse(responseContext, Some(1))
      val x = s.getData(response)
      x.asInstanceOf[Option[Int]] must be(Some(1))

      val response2 = OptionalIntResponse(responseContext, None)
      val x2 = s.getData(response2)
      x2.asInstanceOf[Option[Int]] must be(None)

    }

    "Serailize primitive date response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[DateResponse].typeSymbol.asClass)

      s.responseDataType must be(ResponseDataType.Primitive)

      val date = DateUtil.parseISO8601Date("2010-01-02T10:20:30Z")
      val response = DateResponse(responseContext, date)
      val x = s.getData(response)

      x.asInstanceOf[Date].getTime must be(date.getTime)
    }

    "Serailize object response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[ObjectResponse].typeSymbol.asClass)

      s.responseDataType must be(ResponseDataType.Object)

      val pet = Pet("spot", "dog")
      val response = ObjectResponse(responseContext, pet)
      val x = s.getData(response)

      x.asInstanceOf[Pet].name must be("spot")
    }

    "Serailize byte array response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[BytesResponse].typeSymbol.asClass)

      s.responseDataType must be(ResponseDataType.Bytes)

      val response = BytesResponse(responseContext, Seq(1, 2, 3))
      val x = s.getData(response)

      x.asInstanceOf[Seq[Byte]].length must be(3)
    }

    "Serailize url response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[UrlResponse].typeSymbol.asClass)

      s.responseDataType must be(ResponseDataType.URL)

      val response = UrlResponse(responseContext, new URL("file://c://temp//test.txt"))
      val x = s.getData(response)

      x.asInstanceOf[URL].toString() must be("file://c://temp//test.txt")
    }

    "throw error for Responses where the 1st parameter is not called 'context'" in {
      val thrown = intercept[RestDefintionException] {
        RestResponseSerializer(
          config,
          mirror,
          SerializerGenericRegistration,
          ru.typeOf[FirstParamNotCalledContextResponse].typeSymbol.asClass)
      }
      thrown.getMessage must be("First constructor parameter of 'org.mashupbots.socko.rest.FirstParamNotCalledContextResponse' must be called 'context'.")
    }

    "throw error for Responses with no constructor params" in {
      val thrown = intercept[RestDefintionException] {
        RestResponseSerializer(
          config,
          mirror,
          SerializerGenericRegistration,
          ru.typeOf[NoParamsResponse].typeSymbol.asClass)
      }
      thrown.getMessage must be("'org.mashupbots.socko.rest.NoParamsResponse' constructor must have parameters.")
    }

  }

}

object SerializerGenericRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class VoidResponse(context: RestResponseContext) extends RestResponse

case class StringResponse(context: RestResponseContext, data: String) extends RestResponse
case class OptionalIntResponse(context: RestResponseContext, data: Option[Int]) extends RestResponse

case class DateResponse(context: RestResponseContext, data: Date) extends RestResponse

case class Pet(name: String, description: String)
case class ObjectResponse(context: RestResponseContext, data: Pet) extends RestResponse

case class BytesResponse(context: RestResponseContext, data: Seq[Byte]) extends RestResponse
case class UrlResponse(context: RestResponseContext, data: URL) extends RestResponse

// Error no parameters
case class NoParamsResponse() extends RestResponse {
  val context: RestResponseContext = null
}

// Error first parameter not called context
case class FirstParamNotCalledContextResponse(id: String, context: RestResponseContext) extends RestResponse
