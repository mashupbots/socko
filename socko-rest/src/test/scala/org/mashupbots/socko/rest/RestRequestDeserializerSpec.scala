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

import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import scala.reflect.runtime.{ universe => ru }
import java.util.UUID
import org.mashupbots.socko.events.EndPoint
import java.util.Date
import org.mashupbots.socko.infrastructure.DateUtil

class RestRequestDeserializerSpec extends WordSpec with ShouldMatchers with GivenWhenThen with Logger {

  "RestRequestDeserializerSpec" should {

    val mirror = ru.runtimeMirror(getClass.getClassLoader)

    "Parse path parameters with 1 variable" in {

      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("PUT", "/pets/{id}", "/actor/path"),
        ru.typeOf[PathParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length should be(1)
      d.requestParamBindings(0).name should be("id")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/path/1234"), Map.empty)
      val req = d.deserialize(ctx).asInstanceOf[PathParamRequest1]
      req.id should be("1234")
    }

    "Parse path parameters with multiple variables" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("PUT", "/pets/{aaa}/stuff/{format}", "/actor/path"),
        ru.typeOf[PathParamRequest2].typeSymbol.asClass)
      d.requestParamBindings.length should be(2)
      d.requestParamBindings(0).name should be("aaa")
      d.requestParamBindings(0).description should be("test2")
      d.requestParamBindings(1).name should be("format")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/path/5555/stuff/json/1.2/2.2"), Map.empty)
      val req = d.deserialize(ctx).asInstanceOf[PathParamRequest2]
      req.id should be(5555)
      req.format should be("json")
    }

    "Parse query string parameters" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("GET", "/pets/{format}", "/actor/path"),
        ru.typeOf[QueryStringParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length should be(5)
      d.requestParamBindings(0).name should be("format")
      d.requestParamBindings(1).name should be("number")
      d.requestParamBindings(2).name should be("string")
      d.requestParamBindings(2).description should be("hello")
      d.requestParamBindings(3).name should be("exist")
      d.requestParamBindings(4).name should be("notexist")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/pets/json?number=1&string=hello&exist=world"), Map.empty)
      val req = d.deserialize(ctx).asInstanceOf[QueryStringParamRequest1]
      req.number should be(1)
      req.s should be("hello")
      req.exist should be(Some("world"))
      req.notexist should be(None)
    }

    "Parse header parameters" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("GET", "/pets/{format}", "/actor/path"),
        ru.typeOf[HeaderParamRequest1].typeSymbol.asClass)
      d.requestParamBindings.length should be(5)
      d.requestParamBindings(0).name should be("format")
      d.requestParamBindings(1).name should be("number")
      d.requestParamBindings(2).name should be("string")
      d.requestParamBindings(2).description should be("hello")
      d.requestParamBindings(3).name should be("exist")
      d.requestParamBindings(4).name should be("notexist")

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/pets/json"),
        Map("number" -> "1", "string" -> "hello", "exist" -> "world"))

      val req = d.deserialize(ctx).asInstanceOf[HeaderParamRequest1]
      req.number should be(1)
      req.s should be("hello")
      req.exist should be(Some("world"))
      req.notexist should be(None)
    }

    "Parse all data types" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("GET", "/pets", "/actor/path"),
        ru.typeOf[AllDataTypeRequest].typeSymbol.asClass)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/pets/json"),
        Map("string" -> "s", 
            "int" -> "2000000",
            "byte" -> "1",
            "bool" -> "true",
            "short" -> "200",
            "long" -> "10000000",
            "float" -> "1.1",
            "double" -> "2.2",
            "date" -> "2001-07-04T12:08:56.235-07:00"))

      val req = d.deserialize(ctx).asInstanceOf[AllDataTypeRequest]
      req.string should be("s")
      req.int should be("2000000".toInt)
      req.byte should be("1".toByte)
      req.bool should be(true)
      req.short should be("200".toShort)
      req.long should be("10000000".toLong)
      req.float should be("1.1".toFloat)
      req.double should be("2.2".toDouble)
      req.date should be(DateUtil.parseISO8601Date("2001-07-04T12:08:56.235-07:00"))
    }

    "Parse all optional data types" in {
      val d = RestRequestDeserializer(
        mirror,
        RestOperationDef("GET", "/pets", "/actor/path"),
        ru.typeOf[AllOptionalDataTypeRequest].typeSymbol.asClass)

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/pets/json"),
        Map("string" -> "s", 
            "int" -> "2000000",
            "byte" -> "1",
            "bool" -> "true",
            "short" -> "200",
            "long" -> "10000000",
            "float" -> "1.1",
            "double" -> "2.2",
            "date" -> "2001-07-04"))

      val req = d.deserialize(ctx).asInstanceOf[AllOptionalDataTypeRequest]
      req.string should be(Some("s"))
      req.int should be(Some("2000000".toInt))
      req.byte should be(Some("1".toByte))
      req.bool should be(Some(true))
      req.short should be(Some("200".toShort))
      req.long should be(Some("10000000".toLong))
      req.float should be(Some("1.1".toFloat))
      req.double should be(Some("2.2".toDouble))
      req.date should be(Some(DateUtil.parseISO8601Date("2001-07-04")))
    
      val ctx2 = RestRequestContext(EndPoint("GET", "localhost", "/pets/json"), Map.empty)

      val req2 = d.deserialize(ctx2).asInstanceOf[AllOptionalDataTypeRequest]
      req2.string should be(None)
      req2.int should be(None)
      req2.byte should be(None)
      req2.bool should be(None)
      req2.short should be(None)
      req2.long should be(None)
      req2.float should be(None)
      req2.double should be(None)      
      req2.date should be(None)      
    }    
  }
}

case class PathParamRequest1(context: RestRequestContext,
  @RestPath() id: String) extends RestRequest

case class PathParamRequest2(context: RestRequestContext,
  @RestPath(name = "aaa", description = "test2") id: Int,
  @RestPath() format: String) extends RestRequest

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
    