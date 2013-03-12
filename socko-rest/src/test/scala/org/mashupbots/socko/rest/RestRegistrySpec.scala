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
      r.operations.length should be (3)
      
      r.operations.exists(op => op.definition.method == "GET" && 
          op.definition.uriTemplate == "/pets" &&
          op.definition.actorPath == "/my/actor/path" &&
          op.requestClass.fullName == "org.mashupbots.socko.rest.test1.GetPetsRequest" &&
          op.responseClass.fullName == "org.mashupbots.socko.rest.test1.GetPetsResponse") should be (true)
      
      r.operations.exists(op => op.definition.method == "GET" && 
          op.definition.uriTemplate == "/dogs1" &&
          op.definition.actorPath == "/my/actor/path1" &&
          op.requestClass.fullName == "org.mashupbots.socko.rest.test1.GetDogs1Request" &&
          op.responseClass.fullName == "org.mashupbots.socko.rest.test1.GetFunnyNameDogResponse") should be (true)
      
      r.operations.exists(op => op.definition.method == "GET" && 
          op.definition.uriTemplate == "/dogs2" &&
          op.definition.actorPath == "/my/actor/path2" &&
          op.definition.errorResponses.size == 2 &&
          op.requestClass.fullName == "org.mashupbots.socko.rest.test1.GetDogs2Request" &&
          op.responseClass.fullName == "org.mashupbots.socko.rest.test1.GetFunnyNameDogResponse") should be (true)
    }
    
  }
}