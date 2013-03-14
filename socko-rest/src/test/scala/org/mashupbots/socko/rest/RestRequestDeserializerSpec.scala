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

      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/path/5555/stuff/json"), Map.empty)
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

  }
}

case class PathParamRequest1(context: RestRequestContext,
  @Path() id: String) extends RestRequest

case class PathParamRequest2(context: RestRequestContext,
  @Path(name = "aaa", description = "test2") id: Int,
  @Path() format: String) extends RestRequest

case class QueryStringParamRequest1(context: RestRequestContext,
  @Path() format: String,
  @QueryString() number: Int,
  @QueryString(name = "string", description = "hello") s: String,
  @QueryString() exist: Option[String],
  @QueryString() notexist: Option[Int]) extends RestRequest

  