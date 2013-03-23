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
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.mashupbots.socko.events.HttpResponseStatus

class RestResponseSerializerSpec extends WordSpec with ShouldMatchers with GivenWhenThen with Logger {

  "RestResponseSerializerSpec" should {

    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val requestContext = RestRequestContext(EndPoint("GET", "localhost", "/path/1234"), Map.empty)
    val responseContext = RestResponseContext(requestContext, HttpResponseStatus(200), Map.empty)

    "Serailize void response" in {
      val s = RestResponseSerializer(
        mirror,
        RestOperationDef("PUT", "/pets/{id}", "/actor/path"),
        ru.typeOf[VoidResponse].typeSymbol.asClass)

      s.responseDataType should be(ResponseDataType.Void)
      s.responseDataTerm should be(None)
    }

    "Serailize primitive string response" in {
      val s = RestResponseSerializer(
        mirror,
        RestOperationDef("PUT", "/pets/{id}", "/actor/path"),
        ru.typeOf[StringResponse].typeSymbol.asClass)

      s.responseDataType should be(ResponseDataType.Primitive)

      val response = StringResponse(responseContext, "hello")
      val x = s.getData(response)

      x.isDefined should be(true)
      x.get.asInstanceOf[String] should be("hello")
    }

    "Serailize primitive date response" in {
      val s = RestResponseSerializer(
        mirror,
        RestOperationDef("PUT", "/pets/{id}", "/actor/path"),
        ru.typeOf[DateResponse].typeSymbol.asClass)

      s.responseDataType should be(ResponseDataType.Primitive)

      val date = DateUtil.parseISO8601Date("2010-01-02T10:20:30Z")
      val response = DateResponse(responseContext, date)
      val x = s.getData(response)

      x.isDefined should be(true)
      x.get.asInstanceOf[Date].getTime should be(date.getTime)
    }

    "Serailize object response" in {
      val s = RestResponseSerializer(
        mirror,
        RestOperationDef("PUT", "/pets/{id}", "/actor/path"),
        ru.typeOf[ObjectResponse].typeSymbol.asClass)

      s.responseDataType should be(ResponseDataType.Object)

      val pet = Pet("spot", "dog")
      val response = ObjectResponse(responseContext, pet)
      val x = s.getData(response)

      x.isDefined should be(true)
      x.get.asInstanceOf[Pet].name should be("spot")
    }

    "Serailize stream response" in {
      val s = RestResponseSerializer(
        mirror,
        RestOperationDef("PUT", "/pets/{id}", "/actor/path"),
        ru.typeOf[StreamResponse].typeSymbol.asClass)

      s.responseDataType should be(ResponseDataType.InputStream)

      val bais = new ByteArrayInputStream(Array.empty)
      val response = StreamResponse(responseContext, bais)
      val x = s.getData(response)

      x.isDefined should be(true)
      x.get.asInstanceOf[InputStream] should be(bais)
    }    
  }
}
case class VoidResponse(context: RestResponseContext) extends RestResponse

case class StringResponse(context: RestResponseContext, data: String) extends RestResponse

case class DateResponse(context: RestResponseContext, data: Date) extends RestResponse

case class Pet(name: String, description: String)
case class ObjectResponse(context: RestResponseContext, data: Pet) extends RestResponse

case class StreamResponse(context: RestResponseContext, data: ByteArrayInputStream) extends RestResponse
