//
// Copyright 2012 Vibul Imtarnasan, David Bolton and Socko contributors.
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
package org.mashupbots.socko.routes

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.mashupbots.socko.events.EndPoint
import org.scalatest.GivenWhenThen

@RunWith(classOf[JUnitRunner])
class QueryStringRouteSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  "Querystring route extractors" should {
    "route on exact querystring match" in {
      var result = ""
      val r = Routes({
        case PUT(QueryString("name1=value1")) => result = "1"
        case QueryString("name2=value2") => result = "2"
      })

      r(TestContext(EndPoint("GET", "localhost", "/testpath?name2=value2")))
      result should equal("2")

      r(TestContext(EndPoint("PUT", "localhost", "/testpath?name1=value1")))
      result should equal("1")
    }

    "route on querystring regular expression" in {
      object MyQueryStringRegex extends QueryStringRegex("""name1=([a-z0-9]+)""".r)

      var result = ""
      val r = Routes({
        case MyQueryStringRegex(m) => {
          m.group(1) should equal("value1")
          result = "1"
        }
      })

      r(TestContext(EndPoint("PUT", "www.def.com", "/path/to/file?name1=value1")))
      result should equal("1")
    }

    "route on querystring match" in {
      object QueryStringField extends QueryStringField("name1")

      var result = ""
      val r = Routes({
        case QueryStringField(value) => {
          value should equal("value1")
          result = "1"
        }
      })

      when("there is only 1 name-value pair")
      r(TestContext(EndPoint("GET", "www.def.com", "/path/to/file?name1=value1")))
      result should equal("1")      

      when("there are 2 values for the same name")
      r(TestContext(EndPoint("GET", "www.def.com", "/path/to/file?name1=value1&name1=value2")))
      result should equal("1")      

      when("there are 2 name-value pairs")
      r(TestContext(EndPoint("PUT", "www.def.com", "/path/to/file?name1=value1&name2=value2")))
      result should equal("1")
      
      when("there are 2 name-value pairs is different order")
      r(TestContext(EndPoint("GET", "www.def.com", "/path/to/file?name2=value2&name1=value1")))
      result should equal("1")      
    }
    
    "throw MatchError" in {
      var result = ""
      val r = Routes({
        case PUT(QueryString("name1=value1")) => result = "1"
        case QueryString("name2=value2") => result = "2"
      })

      when("there is no exact match")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "/file.html?name3=value3")))
      }

      when("there is no case sensitive match")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "/TestPath?Name1=value1")))
      }
      
      when("there are mulitple paramters")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "/TestPath?name1=value1&name2=value2")))
      }      
    }

  }
}