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
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.Finders
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import akka.actor.ActorRef
import akka.actor.ActorSystem
import org.mashupbots.socko.events.ImmutableHttpHeaders
import org.mashupbots.socko.events.HttpHeader

class RestRequestDeserializerSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestRequestDeserializerSpec" must {
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val config = RestConfig("1.0", "http://localhost/api")

    "Parse path parameters with 1 variable" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        PathParam1RequestRegistration,
        RestEndPoint(config, PathParam1RequestRegistration),
        ru.typeOf[PathParam1Request].typeSymbol.asClass)

      d.requestParamBindings.length must be(1)
      d.requestParamBindings(0).registration.name must be("id")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/1234"),
        ImmutableHttpHeaders.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)
      val req = d.deserialize(ctx).asInstanceOf[PathParam1Request]
      req.id must be("1234")
    }

    "Parse path parameters with multiple variables" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        PathParam2RequestRegistration,
        RestEndPoint(config, PathParam2RequestRegistration),
        ru.typeOf[PathParam2Request].typeSymbol.asClass)

      d.requestParamBindings.length must be(2)
      d.requestParamBindings(0).registration.name must be("id")
      d.requestParamBindings(0).registration.description must be("test2")
      d.requestParamBindings(1).registration.name must be("format")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/5555/stuff/json/1.2/2.2"),
        ImmutableHttpHeaders.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)
      val req = d.deserialize(ctx).asInstanceOf[PathParam2Request]
      req.id must be(5555)
      req.format must be("json")
    }

    "Parse path parameters with invalid data" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        PathParam3RequestRegistration,
        RestEndPoint(config, PathParam3RequestRegistration),
        ru.typeOf[PathParam3Request].typeSymbol.asClass)
      d.requestParamBindings.length must be(1)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/stuff/string_not_int"),
        ImmutableHttpHeaders.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)

      val thrown = intercept[RestBindingException] {
        val req = d.deserialize(ctx).asInstanceOf[PathParam3Request]
      }
      thrown.getMessage must be("Cannot parse 'string_not_int' for path variable 'id' in '/api/path/stuff/string_not_int' for request 'org.mashupbots.socko.rest.PathParam3Request'")
    }

    "Parse query string parameters" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        QueryStringParam1Registration,
        RestEndPoint(config, QueryStringParam1Registration),
        ru.typeOf[QueryStringParam1Request].typeSymbol.asClass)
      d.requestParamBindings.length must be(5)
      d.requestParamBindings(0).registration.name must be("format")
      d.requestParamBindings(1).registration.name must be("number")
      d.requestParamBindings(1).asInstanceOf[QueryStringBinding].queryFieldName must be("number")
      d.requestParamBindings(2).registration.name must be("s")
      d.requestParamBindings(2).asInstanceOf[QueryStringBinding].queryFieldName must be("string")
      d.requestParamBindings(2).registration.description must be("hello")
      d.requestParamBindings(3).registration.name must be("exist")
      d.requestParamBindings(4).registration.name must be("notexist")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json?number=1&string=hello&exist=world"),
        ImmutableHttpHeaders.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)
      val req = d.deserialize(ctx).asInstanceOf[QueryStringParam1Request]
      req.number must be(1)
      req.s must be("hello")
      req.exist must be(Some("world"))
      req.notexist must be(None)
    }

    "Parse header parameters" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        HeaderParam1Registration,
        RestEndPoint(config, HeaderParam1Registration),
        ru.typeOf[HeaderParam1Request].typeSymbol.asClass)
      d.requestParamBindings.length must be(5)
      d.requestParamBindings(0).registration.name must be("format")
      d.requestParamBindings(1).registration.name must be("number")
      d.requestParamBindings(1).asInstanceOf[HeaderBinding].headerFieldName must be("number")
      d.requestParamBindings(2).registration.name must be("s")
      d.requestParamBindings(2).asInstanceOf[HeaderBinding].headerFieldName must be("string")
      d.requestParamBindings(2).registration.description must be("hello")
      d.requestParamBindings(3).registration.name must be("exist")
      d.requestParamBindings(4).registration.name must be("notexist")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"),
        ImmutableHttpHeaders(Seq(("number", "1"), ("string", "hello"), ("exist", "world"))),
        SockoEventType.HttpRequest, config.requestTimeoutSeconds)

      val req = d.deserialize(ctx).asInstanceOf[HeaderParam1Request]
      req.number must be(1)
      req.s must be("hello")
      req.exist must be(Some("world"))
      req.notexist must be(None)
    }

    "Parse all data types" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        AllDataTypeRegistration,
        RestEndPoint(config, AllDataTypeRegistration),
        ru.typeOf[AllDataTypeRequest].typeSymbol.asClass)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"),
        ImmutableHttpHeaders(Seq(
          ("string", "s"),
          ("int", "2000000"),
          ("byte", "1"),
          ("bool", "true"),
          ("short", "200"),
          ("long", "10000000"),
          ("float", "1.1"),
          ("double", "2.2"),
          ("date", "2001-07-04T12:08:56.235-0700"))),
        SockoEventType.HttpRequest,
        config.requestTimeoutSeconds)

      val req = d.deserialize(ctx).asInstanceOf[AllDataTypeRequest]
      req.string must be("s")
      req.int must be("2000000".toInt)
      req.byte must be("1".toByte)
      req.bool must be(true)
      req.short must be("200".toShort)
      req.long must be("10000000".toLong)
      req.float must be("1.1".toFloat)
      req.double must be("2.2".toDouble)
      req.date must be(DateUtil.parseISO8601Date("2001-07-04T12:08:56.235-0700"))
    }

    "Parse all optional data types" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        AllOptionalDataTypeRegistration,
        RestEndPoint(config, AllOptionalDataTypeRegistration),
        ru.typeOf[AllOptionalDataTypeRequest].typeSymbol.asClass)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"),
        ImmutableHttpHeaders(Seq(
          ("string", "s"),
          ("int", "2000000"),
          ("byte", "1"),
          ("bool", "true"),
          ("short", "200"),
          ("long", "10000000"),
          ("float", "1.1"),
          ("double", "2.2"),
          ("date", "2001-07-04"))),
        SockoEventType.HttpRequest,
        config.requestTimeoutSeconds)

      val req = d.deserialize(ctx).asInstanceOf[AllOptionalDataTypeRequest]
      req.string must be(Some("s"))
      req.int must be(Some("2000000".toInt))
      req.byte must be(Some("1".toByte))
      req.bool must be(Some(true))
      req.short must be(Some("200".toShort))
      req.long must be(Some("10000000".toLong))
      req.float must be(Some("1.1".toFloat))
      req.double must be(Some("2.2".toDouble))
      req.date must be(Some(DateUtil.parseISO8601Date("2001-07-04")))

      val ctx2 = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"),
        ImmutableHttpHeaders.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)

      val req2 = d.deserialize(ctx2).asInstanceOf[AllOptionalDataTypeRequest]
      req2.string must be(None)
      req2.int must be(None)
      req2.byte must be(None)
      req2.bool must be(None)
      req2.short must be(None)
      req2.long must be(None)
      req2.float must be(None)
      req2.double must be(None)
      req2.date must be(None)
    }

    "Throw error for Requests without bindings" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          NoParameterRegistration,
          RestEndPoint(config, NoParameterRegistration),
          ru.typeOf[NoParameterRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("'id' in 'org.mashupbots.socko.rest.NoParameterRequest' has not been declared in 'org.mashupbots.socko.rest.NoParameterRegistration'")
    }

    "Throw error for Requests with multiple bindings" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          MultiParameterRegistration,
          RestEndPoint(config, MultiParameterRegistration),
          ru.typeOf[MultiParameterRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("'id' in 'org.mashupbots.socko.rest.MultiParameterRequest' has been declared more than once in 'org.mashupbots.socko.rest.MultiParameterRegistration'")
    }

    "Throw error for Requests with path bindings not defined" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          BadPathParameterRegistration,
          RestEndPoint(config, BadPathParameterRegistration),
          ru.typeOf[BadPathParameterRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("'id' in 'org.mashupbots.socko.rest.BadPathParameterRequest' is not in the path. '/api/path/{format}' does not contain a variable named 'id'.")
    }

    "Throw error for Requests with body bindings to non POST or PUT methods" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          GetBodyBindingRegistration,
          RestEndPoint(config, GetBodyBindingRegistration),
          ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("'data' in 'org.mashupbots.socko.rest.BadBodyBindingRequest' cannot be bound to the request body using a 'GET' operation.")

      val thrown2 = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          DeleteBodyBindingRegistration,
          RestEndPoint(config, DeleteBodyBindingRegistration),
          ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)
      }
      thrown2.getMessage must be("'data' in 'org.mashupbots.socko.rest.BadBodyBindingRequest' cannot be bound to the request body using a 'DELETE' operation.")

      // OK
      RestRequestDeserializer(
        config,
        mirror,
        PutBodyBindingRegistration,
        RestEndPoint(config, PutBodyBindingRegistration),
        ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)

      // OK
      RestRequestDeserializer(
        config,
        mirror,
        PostBodyBindingRegistration,
        RestEndPoint(config, PostBodyBindingRegistration),
        ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)
    }

    "Throw error for Requests where the 1st parameter is not of type RestRequestContext" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          FirstParamNotContextRegistration,
          RestEndPoint(config, FirstParamNotContextRegistration),
          ru.typeOf[FirstParamNotContextRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("First constructor parameter of 'org.mashupbots.socko.rest.FirstParamNotContextRequest' must be of type RestRequestContext.")
    }

    "Throw error for Requests where the 1st parameter is not called 'context'" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          FirstParamNotCalledContextRegistration,
          RestEndPoint(config, FirstParamNotCalledContextRegistration),
          ru.typeOf[FirstParamNotCalledContextRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("First constructor parameter of 'org.mashupbots.socko.rest.FirstParamNotCalledContextRequest' must be called 'context'.")
    }

  }

}

case class PathParam1Request(context: RestRequestContext, id: String) extends RestRequest

object PathParam1RequestRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets/{id}"
  val requestParams = Seq(PathParam("id"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class PathParam2Request(context: RestRequestContext, id: Int, format: String) extends RestRequest
object PathParam2RequestRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets/{id}/stuff/{format}"
  val requestParams = Seq(PathParam("id", "test2"), PathParam("format"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class PathParam3Request(context: RestRequestContext, id: Int) extends RestRequest
object PathParam3RequestRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/path/stuff/{id}"
  val requestParams = Seq(PathParam("id", "test2"), PathParam("format"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class QueryStringParam1Request(context: RestRequestContext,
  format: String,
  number: Int,
  s: String,
  exist: Option[String],
  notexist: Option[Int]) extends RestRequest
object QueryStringParam1Registration extends RestRegistration {
  val method = Method.GET
  val path = "/pets/{format}"
  val requestParams = Seq(PathParam("format"),
    QueryParam("number"),
    QueryParam("s", "hello", "string"),
    QueryParam("exist"),
    QueryParam("notexist"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class HeaderParam1Request(context: RestRequestContext,
  format: String,
  number: Int,
  s: String,
  exist: Option[String],
  notexist: Option[Int]) extends RestRequest
object HeaderParam1Registration extends RestRegistration {
  val method = Method.GET
  val path = "/pets/{format}"
  val requestParams = Seq(PathParam("format"),
    HeaderParam("number"),
    HeaderParam("s", "hello", "string"),
    HeaderParam("exist"),
    HeaderParam("notexist"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class Fish(name: String, age: Int)
case class BodyParam1Request(context: RestRequestContext, fish: Option[Fish]) extends RestRequest
object BodyParam1Registration extends RestRegistration {
  val method = Method.PUT
  val path = "/pets/{format}"
  val requestParams = Seq(BodyParam("fist"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class AllDataTypeRequest(context: RestRequestContext,
  string: String,
  int: Int,
  byte: Byte,
  bool: Boolean,
  short: Short,
  long: Long,
  float: Float,
  double: Double,
  date: Date) extends RestRequest
object AllDataTypeRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets"
  val requestParams = Seq(
    HeaderParam("string"),
    HeaderParam("int"),
    HeaderParam("byte"),
    HeaderParam("bool"),
    HeaderParam("short"),
    HeaderParam("long"),
    HeaderParam("float"),
    HeaderParam("double"),
    HeaderParam("date"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

case class AllOptionalDataTypeRequest(context: RestRequestContext,
  string: Option[String],
  int: Option[Int],
  byte: Option[Byte],
  bool: Option[Boolean],
  short: Option[Short],
  long: Option[Long],
  float: Option[Float],
  double: Option[Double],
  date: Option[Date]) extends RestRequest
object AllOptionalDataTypeRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets"
  val requestParams = Seq(
    HeaderParam("string"),
    HeaderParam("int"),
    HeaderParam("byte"),
    HeaderParam("bool"),
    HeaderParam("short"),
    HeaderParam("long"),
    HeaderParam("float"),
    HeaderParam("double"),
    HeaderParam("date"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error no parameter binding 
case class NoParameterRequest(context: RestRequestContext, id: String) extends RestRequest
object NoParameterRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets/{id}"
  val requestParams = Seq.empty
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error multi parameter binding 
case class MultiParameterRequest(context: RestRequestContext, id: String) extends RestRequest
object MultiParameterRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/pets/{id}"
  val requestParams = Seq(
    PathParam("id"),
    HeaderParam("id"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error - id does not existing in path /path/{format} 
case class BadPathParameterRequest(context: RestRequestContext, id: String) extends RestRequest
object BadPathParameterRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/path/{format}"
  val requestParams = Seq(PathParam("id"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error - Cannot bind body to any method other than POST or PUT 
case class BadBodyBindingRequest(context: RestRequestContext, data: Fish) extends RestRequest
object GetBodyBindingRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/path"
  override val request = Some(ru.typeOf[BadBodyBindingRequest])
  val requestParams = Seq(BodyParam("data"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}
object DeleteBodyBindingRegistration extends RestRegistration {
  val method = Method.DELETE
  val path = "/path"
  override val request = Some(ru.typeOf[BadBodyBindingRequest])
  val requestParams = Seq(BodyParam("data"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}
object PutBodyBindingRegistration extends RestRegistration {
  val method = Method.PUT
  val path = "/path"
  override val request = Some(ru.typeOf[BadBodyBindingRequest])
  val requestParams = Seq(BodyParam("data"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}
object PostBodyBindingRegistration extends RestRegistration {
  val method = Method.POST
  val path = "/path"
  override val request = Some(ru.typeOf[BadBodyBindingRequest])
  val requestParams = Seq(BodyParam("data"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error first parameter not context type
case class FirstParamNotContextRequest(id: String, context: RestRequestContext) extends RestRequest
object FirstParamNotContextRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/path/{id}"
  val requestParams = Seq(BodyParam("id"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}

// Error first parameter not context
case class FirstParamNotCalledContextRequest(ccc: RestRequestContext, id: String, context: RestRequestContext) extends RestRequest
object FirstParamNotCalledContextRegistration extends RestRegistration {
  val method = Method.GET
  val path = "/path/{id}"
  val requestParams = Seq(PathParam("id"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = null
}