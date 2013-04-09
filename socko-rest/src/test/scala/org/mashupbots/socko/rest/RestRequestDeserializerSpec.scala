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
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class RestRequestDeserializerSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestRequestDeserializerSpec" must {

    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val config = RestConfig("1.0", "/api")

    "Parse path parameters with 1 variable" in {

      val d = RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("PUT", "/api", "/pets/{id}", "/actor/path"),
        ru.typeOf[PathParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length must be(1)
      d.requestParamBindings(0).name must be("id")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/1234"),
        Map.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)
      val req = d.deserialize(ctx).asInstanceOf[PathParamRequest1]
      req.id must be("1234")
    }

    "Parse path parameters with multiple variables" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("PUT", "/api", "/pets/{aaa}/stuff/{format}", "/actor/path"),
        ru.typeOf[PathParamRequest2].typeSymbol.asClass)
      d.requestParamBindings.length must be(2)
      d.requestParamBindings(0).name must be("aaa")
      d.requestParamBindings(0).description must be("test2")
      d.requestParamBindings(1).name must be("format")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/5555/stuff/json/1.2/2.2"),
        Map.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)
      val req = d.deserialize(ctx).asInstanceOf[PathParamRequest2]
      req.id must be(5555)
      req.format must be("json")
    }

    "Parse path parameters with invalid data" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("PUT", "/api", "/pets/stuff/{id}", "/actor/path"),
        ru.typeOf[PathParamRequest3].typeSymbol.asClass)
      d.requestParamBindings.length must be(1)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/stuff/stringnotint"),
        Map.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)

      val thrown = intercept[RestBindingException] {
        val req = d.deserialize(ctx).asInstanceOf[PathParamRequest3]
      }
      thrown.getMessage must be("Cannot parse 'stringnotint' for path variable 'id' in '/api/path/stuff/stringnotint' for request 'org.mashupbots.socko.rest.PathParamRequest3'")
    }

    "Parse query string parameters" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("GET", "/api", "/pets/{format}", "/actor/path"),
        ru.typeOf[QueryStringParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length must be(5)
      d.requestParamBindings(0).name must be("format")
      d.requestParamBindings(1).name must be("number")
      d.requestParamBindings(2).name must be("string")
      d.requestParamBindings(2).description must be("hello")
      d.requestParamBindings(3).name must be("exist")
      d.requestParamBindings(4).name must be("notexist")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json?number=1&string=hello&exist=world"),
        Map.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)
      val req = d.deserialize(ctx).asInstanceOf[QueryStringParamRequest1]
      req.number must be(1)
      req.s must be("hello")
      req.exist must be(Some("world"))
      req.notexist must be(None)
    }

    "Parse header parameters" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("GET", "/api", "/pets/{format}", "/actor/path"),
        ru.typeOf[HeaderParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length must be(5)
      d.requestParamBindings(0).name must be("format")
      d.requestParamBindings(1).name must be("number")
      d.requestParamBindings(2).name must be("string")
      d.requestParamBindings(2).description must be("hello")
      d.requestParamBindings(3).name must be("exist")
      d.requestParamBindings(4).name must be("notexist")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"),
        Map("number" -> "1", "string" -> "hello", "exist" -> "world"), SockoEventType.HttpRequest, config.requestTimeoutSeconds)

      val req = d.deserialize(ctx).asInstanceOf[HeaderParamRequest1]
      req.number must be(1)
      req.s must be("hello")
      req.exist must be(Some("world"))
      req.notexist must be(None)
    }

    "Parse all data types" in {
      val d = RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("GET", "/api", "/pets", "/actor/path"),
        ru.typeOf[AllDataTypeRequest].typeSymbol.asClass)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"),
        Map("string" -> "s",
          "int" -> "2000000",
          "byte" -> "1",
          "bool" -> "true",
          "short" -> "200",
          "long" -> "10000000",
          "float" -> "1.1",
          "double" -> "2.2",
          "date" -> "2001-07-04T12:08:56.235-0700"),
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
        RestOperationDef("GET", "/api", "/pets", "/actor/path"),
        ru.typeOf[AllOptionalDataTypeRequest].typeSymbol.asClass)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"),
        Map("string" -> "s",
          "int" -> "2000000",
          "byte" -> "1",
          "bool" -> "true",
          "short" -> "200",
          "long" -> "10000000",
          "float" -> "1.1",
          "double" -> "2.2",
          "date" -> "2001-07-04"),
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
        Map.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)

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

    "Throw error for Requests without annotated bindings" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          RestOperationDef("GET", "/api", "/pets/{id}", "/actor/path"),
          ru.typeOf[NoParameterAnnotationRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("Constructor parameter 'id' of 'org.mashupbots.socko.rest.NoParameterAnnotationRequest' is not annotated with @RestPath, @RestQuery, @RestHeader or @RestBody")
    }

    "Throw error for Requests with multiple annotated bindings" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          RestOperationDef("GET", "/api", "/pets/{id}", "/actor/path"),
          ru.typeOf[MultiParameterAnnotationRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("Constructor parameter 'id' of 'org.mashupbots.socko.rest.MultiParameterAnnotationRequest' has more than one REST annotation")
    }

    "Throw error for Requests with bad path bindings" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          RestOperationDef("GET", "/api", "/pets/{format}", "/actor/path"),
          ru.typeOf[BadPathParameterRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("Constructor parameter 'id' of 'org.mashupbots.socko.rest.BadPathParameterRequest' cannot be bound to the uri template path. '/pets/{format}' does not contain variable named 'id'.")
    }

    "Throw error for Requests with body bindings to non POST or PUT methods" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          RestOperationDef("GET", "/api", "/pets/{format}", "/actor/path"),
          ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("Constructor parameter 'data' of 'org.mashupbots.socko.rest.BadBodyBindingRequest' cannot be bound using @RestBody() for a 'GET' operation.")

      val thrown2 = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          RestOperationDef("DELETE", "/api", "/pets/{format}", "/actor/path"),
          ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)
      }
      thrown2.getMessage must be("Constructor parameter 'data' of 'org.mashupbots.socko.rest.BadBodyBindingRequest' cannot be bound using @RestBody() for a 'DELETE' operation.")

      // OK
      RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("POST", "/api", "/pets/{format}", "/actor/path"),
        ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)

      // OK
      RestRequestDeserializer(
        config,
        mirror,
        RestOperationDef("PUT", "/api", "/pets/{format}", "/actor/path"),
        ru.typeOf[BadBodyBindingRequest].typeSymbol.asClass)
    }

    "Throw error for Requests where the 1st parameter is not of type RestRequestContext" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          RestOperationDef("GET", "/api", "/pets/{format}", "/actor/path"),
          ru.typeOf[FirstParamNotContextRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("First constructor parameter of 'org.mashupbots.socko.rest.FirstParamNotContextRequest' must be of type RestRequestContext.")
    }

    "Throw error for Requests where the 1st parameter is not called 'context'" in {
      val thrown = intercept[RestDefintionException] {
        RestRequestDeserializer(
          config,
          mirror,
          RestOperationDef("GET", "/api", "/pets/{format}", "/actor/path"),
          ru.typeOf[FirstParamNotCalledContextRequest].typeSymbol.asClass)
      }
      thrown.getMessage must be("First constructor parameter of 'org.mashupbots.socko.rest.FirstParamNotCalledContextRequest' must be called 'context'.")
    }

    "Correctly set the timeout period in the context" in {
      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/1234"),
        Map.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)

      ctx.timeoutSeconds must be (config.requestTimeoutSeconds)
      (ctx.timeoutTime.getTime - ctx.startTime.getTime)/1000 must be (config.requestTimeoutSeconds)
    }
  }
}

case class PathParamRequest1(context: RestRequestContext,
  @RestPath() id: String) extends RestRequest

case class PathParamRequest2(context: RestRequestContext,
  @RestPath(name = "aaa", description = "test2") id: Int,
  @RestPath() format: String) extends RestRequest

case class PathParamRequest3(context: RestRequestContext,
  @RestPath() id: Int) extends RestRequest

case class QueryStringParamRequest1(context: RestRequestContext,
  @RestPath() format: String,
  @RestQuery() number: Int,
  @RestQuery(name = "string", description = "hello") s: String,
  @RestQuery() exist: Option[String],
  @RestQuery() notexist: Option[Int]) extends RestRequest

case class HeaderParamRequest1(context: RestRequestContext,
  @RestPath() format: String,
  @RestHeader() number: Int,
  @RestHeader(name = "string", description = "hello") s: String,
  @RestHeader() exist: Option[String],
  @RestHeader() notexist: Option[Int]) extends RestRequest

case class Fish(name: String, age: Int)
case class BodyParamRequest1(context: RestRequestContext,
  @RestBody() fish: Option[Fish]) extends RestRequest

case class AllDataTypeRequest(context: RestRequestContext,
  @RestHeader() string: String,
  @RestHeader() int: Int,
  @RestHeader() byte: Byte,
  @RestHeader() bool: Boolean,
  @RestHeader() short: Short,
  @RestHeader() long: Long,
  @RestHeader() float: Float,
  @RestHeader() double: Double,
  @RestHeader() date: Date) extends RestRequest

case class AllOptionalDataTypeRequest(context: RestRequestContext,
  @RestHeader() string: Option[String],
  @RestHeader() int: Option[Int],
  @RestHeader() byte: Option[Byte],
  @RestHeader() bool: Option[Boolean],
  @RestHeader() short: Option[Short],
  @RestHeader() long: Option[Long],
  @RestHeader() float: Option[Float],
  @RestHeader() double: Option[Double],
  @RestHeader() date: Option[Date]) extends RestRequest

// Error no parameter binding 
case class NoParameterAnnotationRequest(context: RestRequestContext, id: String) extends RestRequest

// Error multi parameter binding 
case class MultiParameterAnnotationRequest(context: RestRequestContext, @RestHeader()@RestPath() id: String) extends RestRequest

// Error - id does not existing in path /path/{format} 
case class BadPathParameterRequest(context: RestRequestContext, @RestPath() id: String) extends RestRequest

// Error - Cannot bind body to any method other than POST or PUT 
case class BadBodyBindingRequest(context: RestRequestContext, @RestBody() data: String) extends RestRequest

// Error first parameter not context type
case class FirstParamNotContextRequest(id: String, context: RestRequestContext) extends RestRequest

// Error first parameter not context
case class FirstParamNotCalledContextRequest(ccc: RestRequestContext, id: String, context: RestRequestContext) extends RestRequest
