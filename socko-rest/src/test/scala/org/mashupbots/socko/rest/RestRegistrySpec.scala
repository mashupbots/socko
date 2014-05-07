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
import org.json4s.native.{Serialization => json}
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.infrastructure.ReflectUtil
import org.scalatest.Finders
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.Matchers

class RestRegistrySpec extends WordSpec with Matchers with GivenWhenThen with Logger {

  "RestRegistry" should {

    val cfg = RestConfig("1.0", "http://localhost/api")
    val rm = ru.runtimeMirror(getClass().getClassLoader())
    val classes = ReflectUtil.getClasses(rm.classLoader, "org.mashupbots.socko.rest.test1")

    "correctly load a valid request" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.GetPetsRegistration")
      val op = RestRegistry.buildRestOperation(rm, clz, classes, cfg)

      op.get.endPoint.method should be("GET")
      op.get.endPoint.relativePath should be("/pets")
      op.get.deserializer.requestClass.fullName should be("org.mashupbots.socko.rest.test1.GetPetsRequest")
      op.get.serializer.responseClass.fullName should be("org.mashupbots.socko.rest.test1.GetPetsResponse")
    }

    "correctly load request with shared custom response class name" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.PostDogs1Registration")
      val op = RestRegistry.buildRestOperation(rm, clz, classes, cfg)

      op.get.endPoint.method should be("POST")
      op.get.endPoint.relativePath should be("/dogs1")
      op.get.deserializer.requestClass.fullName should be("org.mashupbots.socko.rest.test1.PostDogs1Request")
      op.get.serializer.responseClass.fullName should be("org.mashupbots.socko.rest.test1.FunnyNameDogResponse")

      val clz2 = Class.forName("org.mashupbots.socko.rest.test1.PutDogs2Registration")
      val op2 = RestRegistry.buildRestOperation(rm, clz2, classes, cfg)

      op2.get.endPoint.method should be("PUT")
      op2.get.endPoint.relativePath should be("/dogs2")
      op2.get.registration.errors.size should be(2)
      op2.get.deserializer.requestClass.fullName should be("org.mashupbots.socko.rest.test1.PutDogs2Request")
      op2.get.serializer.responseClass.fullName should be("org.mashupbots.socko.rest.test1.FunnyNameDogResponse")
    }

    "correctly load a valid request with bindings" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.DeletePetsRegistration")
      val op = RestRegistry.buildRestOperation(rm, clz, classes, cfg)

      op.get.endPoint.method should be("DELETE")
      op.get.endPoint.relativePath should be("/pets/{id}")
      op.get.deserializer.requestClass.fullName should be("org.mashupbots.socko.rest.test1.DeletePetsRequest")
      op.get.serializer.responseClass.fullName should be("org.mashupbots.socko.rest.test1.DeletePetsResponse")
    }

    "ignore non REST classes" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.NotARestClass")
      RestRegistry.buildRestOperation(rm, clz, classes, cfg) should be(None)
    }

    "throw error for Requests without Responses" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.NoResponseRegistration")

      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, clz, classes, cfg)
      }
      thrown.getMessage should be("Cannot find corresponding RestResponse 'org.mashupbots.socko.rest.test1.NoResponseResponse' for RestRegistration 'org.mashupbots.socko.rest.test1.NoResponseRegistration'")
    }

    "throw error for Requests without parameter binding in registration" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.NoParameterRegistration")

      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, clz, classes, cfg)
      }
      thrown.getMessage should be("'id' in 'org.mashupbots.socko.rest.test1.NoParameterRequest' has not been declared in 'org.mashupbots.socko.rest.test1.NoParameterRegistration'")
    }

    "throw error for Requests more than one parameter binding specified in registration" in {
      val clz = Class.forName("org.mashupbots.socko.rest.test1.MultiParameterRegistration")

      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, clz, classes, cfg)
      }
      thrown.getMessage should be("'id' in 'org.mashupbots.socko.rest.test1.MultiParameterRequest' has been declared more than once in 'org.mashupbots.socko.rest.test1.MultiParameterRegistration'")
    }

    "catch duplcate operation addresses" in {
      val thrown = intercept[RestDefintionException] {
        val r = RestRegistry("org.mashupbots.socko.rest.test2", cfg)
      }
      thrown.getMessage.contains("resolves to the same address as") should be (true)
    }

    "test deserialize" in {

      val s = "{\"name\":\"Boo\",\"age\":5}"
      val formats = json.formats(NoTypeHints)

      val clz = Class.forName("org.mashupbots.socko.rest.Horse")
      val y = org.json4s.reflect.Reflector.scalaTypeOf(clz)
      val z = org.json4s.reflect.ManifestFactory.manifestOf(y)
      val x = json.read(s)(formats, z)

      val hh = x.asInstanceOf[Horse]
      hh.name should be("Boo")
    }

    "test serialize" in {
    	 implicit val formats = json.formats(NoTypeHints)
         val s = json.write("test")         
         s should be ("\"test\"")
    	     	 
         val s2 = json.write("")         
         s2 should be ("\"\"")
    }
    
    "reflect object" in {
      val clz = Class.forName("org.mashupbots.socko.rest.MyObject")
      val rm = ru.runtimeMirror(getClass.getClassLoader)
      val module = rm.moduleSymbol(clz)
      val x = module.isModule
      val moduleMirror = rm.reflectModule(module)

      val obj = moduleMirror.instance

      module.typeSignature <:< ru.typeOf[MyTest] should be(true)
      (obj.asInstanceOf[MyTest]).a should be(1)
      (obj.asInstanceOf[MyTest]).b should be(3)
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
