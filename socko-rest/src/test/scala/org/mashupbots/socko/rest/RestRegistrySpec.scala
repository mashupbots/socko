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
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.mashupbots.socko.infrastructure.ReflectUtil

class RestRegistrySpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestRegistrySpec" must {

    val cfg = RestConfig("1.0", "/api")
    val rm = ru.runtimeMirror(getClass().getClassLoader())
    val classes = ReflectUtil.getClasses(rm.classLoader, "org.mashupbots.socko.rest.test1")
    val classSymbols = classes.map(clz => rm.classSymbol(clz))

    "correctly load a valid request" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.GetPetsRequest"))      
      val op = RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      
      op.get.definition.method must be ("GET")
      op.get.definition.urlTemplate must be ("/pets")
      op.get.definition.dispatcherClass must be ("")
      op.get.deserializer.requestClass.fullName must be ("org.mashupbots.socko.rest.test1.GetPetsRequest")
      op.get.serializer.responseClass.fullName must be ("org.mashupbots.socko.rest.test1.GetPetsResponse")
    } 
    
    "correctly load request with shared custom response class name" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.PostDogs1Request"))      
      val op = RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      
      op.get.definition.method must be ("POST")
      op.get.definition.urlTemplate must be ("/dogs1")
      op.get.definition.dispatcherClass must be ("GetPetsDispatcher")
      op.get.deserializer.requestClass.fullName must be ("org.mashupbots.socko.rest.test1.PostDogs1Request")
      op.get.serializer.responseClass.fullName must be ("org.mashupbots.socko.rest.test1.FunnyNameDogResponse")
      
      val cs2 = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.PutDogs2Request"))      
      val op2 = RestRegistry.buildRestOperation(rm, cs2, classSymbols, cfg)
      
      op2.get.definition.method must be ("PUT")
      op2.get.definition.urlTemplate must be ("/dogs2")
      op2.get.definition.dispatcherClass must be ("org.mashupbots.socko.rest.test1.GetPetsDispatcher")
      op2.get.definition.errorResponses.size must be (2)
      op2.get.deserializer.requestClass.fullName must be ("org.mashupbots.socko.rest.test1.PutDogs2Request")
      op2.get.serializer.responseClass.fullName must be ("org.mashupbots.socko.rest.test1.FunnyNameDogResponse")      
    } 

    "correctly load a valid request with bindings" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.DeletePetsRequest"))      
      val op = RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      
      op.get.definition.method must be ("DELETE")
      op.get.definition.urlTemplate must be ("/pets/{id}")
      op.get.definition.dispatcherClass must be ("")
      op.get.deserializer.requestClass.fullName must be ("org.mashupbots.socko.rest.test1.DeletePetsRequest")
      op.get.serializer.responseClass.fullName must be ("org.mashupbots.socko.rest.test1.DeletePetsResponse")
    } 
    
    "ignore non REST classes" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.NotARestClass"))
      RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg) must be (None)
    }
    
    "throw error for Requests without Responses" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.NoResponseRequest"))
      
      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      }
      thrown.getMessage must be("Cannot find corresponding RestResponse 'org.mashupbots.socko.rest.test1.NoResponseResponse' for RestRequest 'org.mashupbots.socko.rest.test1.NoResponseRequest'")
    }
    
    "throw error for Requests without Annotations" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.NoAnnotationRequest"))
      
      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      }
      thrown.getMessage must be("'org.mashupbots.socko.rest.test1.NoAnnotationRequest' extends RestRequest but is not annotated with a REST operation like '@RestGet'")
    }

    "throw error for Requests without Annotationed parameters" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.NoParameterAnnotationRequest"))
      
      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      }
      thrown.getMessage must be("Constructor parameter 'id' of 'org.mashupbots.socko.rest.test1.NoParameterAnnotationRequest' is not annotated with @RestPath, @RestQuery, @RestHeader or @RestBody")
    }
    
    "throw error for Requests more than one Annotationed parameters" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.MultiParameterAnnotationRequest"))
      
      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      }
      thrown.getMessage must be("Constructor parameter 'id' of 'org.mashupbots.socko.rest.test1.MultiParameterAnnotationRequest' has more than one REST annotation")
    }

    "throw error for Annotationed class that is not a RestRequest" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.NotARequest"))
      
      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      }
      thrown.getMessage must be("'org.mashupbots.socko.rest.test1.NotARequest' is annotated with '@RestGet' but does not extend RestRequest")
    }

    "throw error for when there is not a dispatcher" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.GetNoDispatcherRequest"))
      
      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      }
      thrown.getMessage must be("Cannot find corresponding RestDispatcher 'org.mashupbots.socko.rest.test1.GetNoDispatcherDispatcher' for RestRequest 'org.mashupbots.socko.rest.test1.GetNoDispatcherRequest'")
    }

    "throw error for when there is not a dispatcher with zero parameter constructor" in {
      val cs = rm.classSymbol(Class.forName("org.mashupbots.socko.rest.test1.GetBadDispatcherRequest"))
      
      val thrown = intercept[RestDefintionException] {
        RestRegistry.buildRestOperation(rm, cs, classSymbols, cfg)
      }
      thrown.getMessage must be("RestDispatcher 'org.mashupbots.socko.rest.test1.GetBadDispatcherDispatcher' for RestRequest 'org.mashupbots.socko.rest.test1.GetBadDispatcherRequest' does not have a parameterless constructor")
    }
    
    "catch duplcate operation addresses" in {
      val thrown = intercept[RestDefintionException] {
        val r = RestRegistry("org.mashupbots.socko.rest.test2", cfg)
      }
      thrown.getMessage must be("Operation 'GET /pets' for 'org.mashupbots.socko.rest.test2.GetPets1Request' resolves to the same address as 'GET /pets' for 'org.mashupbots.socko.rest.test2.GetPets2Request'")
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

  }
}

case class Horse(name: String, age: Int)
