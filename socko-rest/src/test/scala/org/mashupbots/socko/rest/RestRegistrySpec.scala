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

class RestRegistrySpec extends WordSpec with ShouldMatchers with GivenWhenThen with Logger {

  "RestRegistrySpec" should {
    
    "correctly find operations" in  {
      val r = RestRegistry("org.mashupbots.socko.rest.test1")
      r.endPoints.length should be (3)
      
      r.endPoints.exists(e => e.operation.method == "GET" && 
          e.operation.uriTemplate == "/pets" &&
          e.operation.actorPath == "/my/actor/path" &&
          e.requestClass.fullName == "org.mashupbots.socko.rest.test1.GetPetsRequest" &&
          e.responseClass.fullName == "org.mashupbots.socko.rest.test1.GetPetsResponse") should be (true)
      
      r.endPoints.exists(e => e.operation.method == "GET" && 
          e.operation.uriTemplate == "/dogs1" &&
          e.operation.actorPath == "/my/actor/path1" &&
          e.requestClass.fullName == "org.mashupbots.socko.rest.test1.GetDogs1Request" &&
          e.responseClass.fullName == "org.mashupbots.socko.rest.test1.GetFunnyNameDogResponse") should be (true)
      
      r.endPoints.exists(e => e.operation.method == "GET" && 
          e.operation.uriTemplate == "/dogs2" &&
          e.operation.actorPath == "/my/actor/path2" &&
          e.operation.errorResponses.size == 2 &&
          e.requestClass.fullName == "org.mashupbots.socko.rest.test1.GetDogs2Request" &&
          e.responseClass.fullName == "org.mashupbots.socko.rest.test1.GetFunnyNameDogResponse") should be (true)
    }
    
  }
}