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
import org.scalatest.matchers.MustMatchers

class RestOperationDefSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestOperationSpec" should {
    
    "compare endpoint address with different methods" in  {
      RestOperationDef("GET", "/api", "/pets" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("PUT", "/api", "/pets" ,"/actor/path")) must be (false)
      
      RestOperationDef("PUT", "/api", "/pets/{id}" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("POST", "/api", "/pets/{id}" ,"/actor/path")) must be (false)
      
      RestOperationDef("PUT", "/api", "/pets/{id}" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("POST", "/api", "/{type}/{id}" ,"/actor/path")) must be (false)      
    }

    "compare endpoint address with different paths" in  {
      RestOperationDef("GET", "/api", "/pets" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("GET", "/api", "/pets" ,"/actor/path")) must be (true)
      
      RestOperationDef("GET", "/api", "/pets" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("GET", "/api", "/dogs" ,"/actor/path")) must be (false)

      RestOperationDef("GET", "/api", "/pets" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("GET", "/api", "/pets/dogs" ,"/actor/path")) must be (false)

      RestOperationDef("PUT", "/api", "/pets/{id}" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("PUT", "/api", "/pets/{id}" ,"/actor/path")) must be (true)

      // Mix of variable and static in 1st path segment makes it the same address
      RestOperationDef("PUT", "/api", "/pets/{id}" ,"/actor/path").compareUrlTemplate(
          RestOperationDef("PUT", "/api", "/{type}/{id}" ,"/actor/path")) must be (true)
      
      
    }
    
  }
}