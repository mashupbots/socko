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

import scala.reflect.runtime.{ universe => ru }

import org.json4s.NoTypeHints
import org.json4s.native.{ Serialization => json }
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.ReflectUtil
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class RestRegistrySpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestRegistry" must {

    val cfg = RestConfig("1.0", "/api")
    val rm = ru.runtimeMirror(getClass().getClassLoader())
    val classes = ReflectUtil.getClasses(rm.classLoader, "org.mashupbots.socko.rest.test1")

    "correctly load a valid request" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.GetPetsDeclaration")
      val op = RestRegistry.buildRestOperation(rm, clz, classes, cfg)

      op.get.endPoint.method must be("GET")
      op.get.endPoint.relativePath must be("/pets")
      op.get.deserializer.requestClass.fullName must be("org.mashupbots.socko.rest.test1.GetPetsRequest")
      op.get.serializer.responseClass.fullName must be("org.mashupbots.socko.rest.test1.GetPetsResponse")
    }

    "correctly load request with shared custom response class name" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.PostDogs1Declaration")
      val op = RestRegistry.buildRestOperation(rm, clz, classes, cfg)

      op.get.endPoint.method must be("POST")
      op.get.endPoint.relativePath must be("/dogs1")
      op.get.deserializer.requestClass.fullName must be("org.mashupbots.socko.rest.test1.PostDogs1Request")
      op.get.serializer.responseClass.fullName must be("org.mashupbots.socko.rest.test1.FunnyNameDogResponse")

      val clz2 = Class.forName("org.mashupbots.socko.rest.test1.PutDogs2Declaration")
      val op2 = RestRegistry.buildRestOperation(rm, clz2, classes, cfg)

      op2.get.endPoint.method must be("PUT")
      op2.get.endPoint.relativePath must be("/dogs2")
      op2.get.declaration.errors.size must be(2)
      op2.get.deserializer.requestClass.fullName must be("org.mashupbots.socko.rest.test1.PutDogs2Request")
      op2.get.serializer.responseClass.fullName must be("org.mashupbots.socko.rest.test1.FunnyNameDogResponse")
    }

    "correctly load a valid request with bindings" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.DeletePetsDeclaration")
      val op = RestRegistry.buildRestOperation(rm, clz, classes, cfg)

      op.get.endPoint.method must be("DELETE")
      op.get.endPoint.relativePath must be("/pets/{id}")
      op.get.deserializer.requestClass.fullName must be("org.mashupbots.socko.rest.test1.DeletePetsRequest")
      op.get.serializer.responseClass.fullName must be("org.mashupbots.socko.rest.test1.DeletePetsResponse")
    }

    "ignore non REST classes" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.NotARestClass")
      RestRegistry.buildRestOperation(rm, clz, classes, cfg) must be(None)
    }

    "throw error for Requests without Responses" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.NoResponseDeclaration")

      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, clz, classes, cfg)
      }
      thrown.getMessage must be("Cannot find corresponding RestResponse 'org.mashupbots.socko.rest.test1.NoResponseResponse' for RestDeclaration 'org.mashupbots.socko.rest.test1.NoResponseDeclaration'")
    }

    "throw error for Requests without parameter binding in declaration" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.NoParameterDeclaration")

      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, clz, classes, cfg)
      }
      thrown.getMessage must be("'id' in 'org.mashupbots.socko.rest.test1.NoParameterRequest' has not been declared in 'org.mashupbots.socko.rest.test1.NoParameterDeclaration'")
    }

    "throw error for Requests more than one parameter binding specified in declaration" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.MultiParameterDeclaration")

      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, clz, classes, cfg)
      }
      thrown.getMessage must be("'id' in 'org.mashupbots.socko.rest.test1.MultiParameterRequest' has been declared more than once in 'org.mashupbots.socko.rest.test1.MultiParameterDeclaration'")
    }

    "catch duplcate operation addresses" in {
      val thrown = intercept[RestDefintionException] {
        val r = RestRegistry("org.mashupbots.socko.rest.test2", cfg)
      }
      thrown.getMessage must be("Operation 'GET /pets' for 'org.mashupbots.socko.rest.test2.GetPets2Request' resolves to the same address as 'GET /pets' for 'org.mashupbots.socko.rest.test2.GetPets1Request'")
    }

    "test deserialize" in {

      val s = "{\"name\":\"Boo\",\"age\":5}"
      val formats = json.formats(NoTypeHints)

      val clz = Class.forName("org.mashupbots.socko.rest.Horse")
      val y = org.json4s.reflect.Reflector.scalaTypeOf(clz)
      val z = org.json4s.reflect.ManifestFactory.manifestOf(y)
      val x = json.read(s)(formats, z)

      val hh = x.asInstanceOf[Horse]
      hh.name must be("Boo")
    }

    "reflect object" in {
      val clz = Class.forName("org.mashupbots.socko.rest.MyObject")
      val rm = ru.runtimeMirror(getClass.getClassLoader)
      val module = rm.moduleSymbol(clz)
      val x = module.isModule
      val moduleMirror = rm.reflectModule(module)

      val obj = moduleMirror.instance

      module.typeSignature <:< ru.typeOf[MyTest] must be(true)
      (obj.asInstanceOf[MyTest]).a must be(1)
      (obj.asInstanceOf[MyTest]).b must be(3)
    }
  }
}

case class Horse(name: String, age: Int)

abstract class MyTest {
  def a: Int
  val b = 2
}

object MyObject extends MyTest {
  val a = 1
  override val b = 3
}

case class MyObject(b: Int)
