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
import org.mashupbots.socko.events.HttpContent
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.DateUtil
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.jboss.netty.buffer.ChannelBuffers
import org.mashupbots.socko.infrastructure.CharsetUtil

class RestRequestDeserializerSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestRequestDeserializerSpec" must {

    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val noBody = HttpContent(None, "")

    "Parse path parameters with 1 variable" in {

      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("PUT", "/api", "/pets/{id}", "/actor/path"),
        ru.typeOf[PathParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length must be(1)
      d.requestParamBindings(0).name must be("id")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/1234"), Map.empty)
      val req = d.deserialize(ctx, noBody).asInstanceOf[PathParamRequest1]
      req.id must be("1234")
    }

    "Parse path parameters with multiple variables" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("PUT", "/api", "/pets/{aaa}/stuff/{format}", "/actor/path"),
        ru.typeOf[PathParamRequest2].typeSymbol.asClass)
      d.requestParamBindings.length must be(2)
      d.requestParamBindings(0).name must be("aaa")
      d.requestParamBindings(0).description must be("test2")
      d.requestParamBindings(1).name must be("format")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/5555/stuff/json/1.2/2.2"), Map.empty)
      val req = d.deserialize(ctx, noBody).asInstanceOf[PathParamRequest2]
      req.id must be(5555)
      req.format must be("json")
    }

    "Parse path parameters with invalid data" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("PUT", "/api", "/pets/stuff/{id}", "/actor/path"),
        ru.typeOf[PathParamRequest3].typeSymbol.asClass)
      d.requestParamBindings.length must be(1)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/stuff/stringnotint"), Map.empty)

      val thrown = intercept[RestBindingException] {
        val req = d.deserialize(ctx, noBody).asInstanceOf[PathParamRequest3]
      }
      thrown.getMessage must be("Cannot parse 'stringnotint' for path variable 'id' in '/api/path/stuff/stringnotint' for request 'org.mashupbots.socko.rest.PathParamRequest3'")
    }

    "Parse query string parameters" in {
      val d = RestRequestDeserializer(
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

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json?number=1&string=hello&exist=world"), Map.empty)
      val req = d.deserialize(ctx, noBody).asInstanceOf[QueryStringParamRequest1]
      req.number must be(1)
      req.s must be("hello")
      req.exist must be(Some("world"))
      req.notexist must be(None)
    }

    "Parse header parameters" in {
      val d = RestRequestDeserializer(
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
        Map("number" -> "1", "string" -> "hello", "exist" -> "world"))

      val req = d.deserialize(ctx, noBody).asInstanceOf[HeaderParamRequest1]
      req.number must be(1)
      req.s must be("hello")
      req.exist must be(Some("world"))
      req.notexist must be(None)
    }

    "Parse body parameters" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("POST", "/api", "/fish", "/actor/path"),
        ru.typeOf[BodyParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length must be(1)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json?number=1&string=hello&exist=world"), Map.empty)
      val buf = ChannelBuffers.wrappedBuffer("{\"name\":\"Boo\",\"age\":5}".getBytes(CharsetUtil.UTF_8))
      val body = HttpContent(Some(buf), "application/json; charset=UTF-8")
      val req = d.deserialize(ctx, body).asInstanceOf[BodyParamRequest1]
      req.fish.isDefined must be(true)
      req.fish.get.name must be("Boo")
      req.fish.get.age must be(5)
    }
    
    "Parse all data types" in {
      val d = RestRequestDeserializer(
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
          "date" -> "2001-07-04T12:08:56.235-07:00"))

      val req = d.deserialize(ctx, noBody).asInstanceOf[AllDataTypeRequest]
      req.string must be("s")
      req.int must be("2000000".toInt)
      req.byte must be("1".toByte)
      req.bool must be(true)
      req.short must be("200".toShort)
      req.long must be("10000000".toLong)
      req.float must be("1.1".toFloat)
      req.double must be("2.2".toDouble)
      req.date must be(DateUtil.parseISO8601Date("2001-07-04T12:08:56.235-07:00"))
    }

    "Parse all optional data types" in {
      val d = RestRequestDeserializer(
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
          "date" -> "2001-07-04"))

      val req = d.deserialize(ctx, noBody).asInstanceOf[AllOptionalDataTypeRequest]
      req.string must be(Some("s"))
      req.int must be(Some("2000000".toInt))
      req.byte must be(Some("1".toByte))
      req.bool must be(Some(true))
      req.short must be(Some("200".toShort))
      req.long must be(Some("10000000".toLong))
      req.float must be(Some("1.1".toFloat))
      req.double must be(Some("2.2".toDouble))
      req.date must be(Some(DateUtil.parseISO8601Date("2001-07-04")))

      val ctx2 = RestRequestContext(EndPoint("GET", "localhost", "/api/pets/json"), Map.empty)

      val req2 = d.deserialize(ctx2, noBody).asInstanceOf[AllOptionalDataTypeRequest]
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
    