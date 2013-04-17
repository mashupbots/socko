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

import java.util.Date

import scala.reflect.runtime.{ universe => ru }

import org.mashupbots.socko.events.EndPoint
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.Finders
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import akka.actor.ActorRef
import akka.actor.ActorSystem

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

      s.dataSerializer.isInstanceOf[VoidDataSerializer] must be(true)

      s.dataSerializer.getData(null) == null must be (true)

      s.dataSerializer.serialize(null) must be (Array.empty)      
    }

    "Serailize primitive string response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[StringResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[PrimitiveDataSerializer] must be(true)

      val response = StringResponse(responseContext, "hello")
      val data = s.dataSerializer.getData(response)
      data.asInstanceOf[String] must be("hello")

      val bytes = s.dataSerializer.serialize(data)
      new String(bytes, CharsetUtil.UTF_8) must be("\"hello\"")
    }

    "Serailize primitive optional string response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[OptionalStringResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[PrimitiveDataSerializer] must be(true)

      // Some
      val response = OptionalStringResponse(responseContext, Some("hello"))
      val i1 = s.dataSerializer.getData(response)
      i1.asInstanceOf[Option[String]] must be(Some("hello"))

      val b1 = s.dataSerializer.serialize(i1)
      new String(b1, CharsetUtil.UTF_8) must be("\"hello\"")

      // None
      val response2 = OptionalStringResponse(responseContext, None)
      val i2 = s.dataSerializer.getData(response2)
      i2.asInstanceOf[Option[String]] must be(None)

      val b2 = s.dataSerializer.serialize(i2)
      //Note no content is returned rather than empty string
      new String(b2, CharsetUtil.UTF_8) must be("")
    }

    "Serailize primitive int response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[IntResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[PrimitiveDataSerializer] must be(true)

      val response = IntResponse(responseContext, 123)
      val data = s.dataSerializer.getData(response)
      data.asInstanceOf[Int] must be(123)

      val bytes = s.dataSerializer.serialize(data)
      new String(bytes, CharsetUtil.UTF_8) must be("123")
    }

    "Serailize primitive optional int response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[OptionalIntResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[PrimitiveDataSerializer] must be(true)

      // Some
      val response = OptionalIntResponse(responseContext, Some(1))
      val i1 = s.dataSerializer.getData(response)
      i1.asInstanceOf[Option[Int]] must be(Some(1))

      val b1 = s.dataSerializer.serialize(i1)
      new String(b1, CharsetUtil.UTF_8) must be("1")

      // None
      val response2 = OptionalIntResponse(responseContext, None)
      val i2 = s.dataSerializer.getData(response2)
      i2.asInstanceOf[Option[Int]] must be(None)

      val b2 = s.dataSerializer.serialize(i2)
      new String(b2, CharsetUtil.UTF_8) must be("")
    }

    "Serailize primitive date response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[DateResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[PrimitiveDataSerializer] must be(true)

      val date = DateUtil.parseISO8601Date("2010-01-02T10:20:30Z")
      val response = DateResponse(responseContext, date)
      val data = s.dataSerializer.getData(response)
      data.asInstanceOf[Date].getTime must be(date.getTime)

      val bytes = s.dataSerializer.serialize(data)
      new String(bytes, CharsetUtil.UTF_8) must be("\"2010-01-02T10:20:30.000Z\"")
    }

    "Serailize primitive optional date response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[OptionalDateResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[PrimitiveDataSerializer] must be(true)

      val date = DateUtil.parseISO8601Date("2010-01-02T10:20:30Z")

      // Some
      val response = OptionalDateResponse(responseContext, Some(date))
      val i1 = s.dataSerializer.getData(response)
      i1.asInstanceOf[Option[Date]] must be(Some(date))

      val b1 = s.dataSerializer.serialize(i1)
      new String(b1, CharsetUtil.UTF_8) must be("\"2010-01-02T10:20:30.000Z\"")

      // None
      val response2 = OptionalDateResponse(responseContext, None)
      val i2 = s.dataSerializer.getData(response2)
      i2.asInstanceOf[Option[Date]] must be(None)

      val b2 = s.dataSerializer.serialize(i2)
      new String(b2, CharsetUtil.UTF_8) must be("")
    }

    "Serailize object response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[ObjectResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[ObjectDataSerializer] must be(true)

      val pet = Pet("spot", "dog")
      val response = ObjectResponse(responseContext, pet)
      val data = s.dataSerializer.getData(response).asInstanceOf[AnyRef]
      data.asInstanceOf[Pet].name must be("spot")

      val bytes = s.dataSerializer.serialize(data)
      new String(bytes, CharsetUtil.UTF_8) must be("{\"name\":\"spot\",\"description\":\"dog\"}")
    }

    "Serailize optional object response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[OptionalObjectResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[ObjectDataSerializer] must be(true)

      val pet = Pet("spot", "dog")

      // Some
      val response = OptionalObjectResponse(responseContext, Some(pet))
      val i1 = s.dataSerializer.getData(response).asInstanceOf[AnyRef]
      i1.asInstanceOf[Option[Pet]] must be(Some(pet))

      val b1 = s.dataSerializer.serialize(i1)
      new String(b1, CharsetUtil.UTF_8) must be("{\"name\":\"spot\",\"description\":\"dog\"}")

      // None
      val response2 = OptionalObjectResponse(responseContext, None)
      val i2 = s.dataSerializer.getData(response2).asInstanceOf[AnyRef]
      i2.asInstanceOf[Option[Pet]] must be(None)

      val b2 = s.dataSerializer.serialize(i2)
      new String(b2, CharsetUtil.UTF_8) must be("")
    }

    "Serailize byte array response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[ByteArrayResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[ByteArrayDataSerializer] must be(true)

      val response = ByteArrayResponse(responseContext, Array(1, 2, 3))
      val data = s.dataSerializer.getData(response)

      data.asInstanceOf[Array[Byte]].length must be(3)

      val bytes = s.dataSerializer.serialize(data)
      bytes.length must be(3)
      bytes(0) must be(1)
      bytes(1) must be(2)
      bytes(2) must be(3)
    }

    "Serailize byte sequence response" in {
      val s = RestResponseSerializer(
        config,
        mirror,
        SerializerGenericRegistration,
        ru.typeOf[ByteSeqResponse].typeSymbol.asClass)

      s.dataSerializer.isInstanceOf[ByteSeqDataSerializer] must be(true)

      val response = ByteSeqResponse(responseContext, Seq(1, 2, 3))
      val data = s.dataSerializer.getData(response)

      data.asInstanceOf[Seq[Byte]].length must be(3)

      val bytes = s.dataSerializer.serialize(data)
      bytes.length must be(3)
      bytes(0) must be(1)
      bytes(1) must be(2)
      bytes(2) must be(3)
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
case class OptionalStringResponse(context: RestResponseContext, data: Option[String]) extends RestResponse

case class IntResponse(context: RestResponseContext, data: Int) extends RestResponse
case class OptionalIntResponse(context: RestResponseContext, data: Option[Int]) extends RestResponse

case class DateResponse(context: RestResponseContext, data: Date) extends RestResponse
case class OptionalDateResponse(context: RestResponseContext, data: Option[Date]) extends RestResponse

case class Pet(name: String, description: String)
case class ObjectResponse(context: RestResponseContext, data: Pet) extends RestResponse
case class OptionalObjectResponse(context: RestResponseContext, data: Option[Pet]) extends RestResponse

case class ByteArrayResponse(context: RestResponseContext, data: Array[Byte]) extends RestResponse
case class ByteSeqResponse(context: RestResponseContext, data: Seq[Byte]) extends RestResponse

// Error no parameters
case class NoParamsResponse() extends RestResponse {
  val context: RestResponseContext = null
}

// Error first parameter not called context
case class FirstParamNotCalledContextResponse(id: String, context: RestResponseContext) extends RestResponse
