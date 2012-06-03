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
import org.mashupbots.socko.events.EndPoint
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec

@RunWith(classOf[JUnitRunner])
class MethodRouteSpec extends WordSpec with ShouldMatchers {

  "Method route extractor" should {
    "route GET method" in {
      var result = false
      val r = Routes({ case GET(x) => result = true })
      r(TestContext(EndPoint("GET", "localhost", "file.html")))
      result should be(true)
    }

    "route PUT method" in {
      var result = false
      val r = Routes({ case PUT(x) => result = true })
      r(TestContext(EndPoint("PUT", "localhost", "file.html")))
      result should be(true)
    }

    "route POST method" in {
      var result = false
      val r = Routes({ case POST(x) => result = true })
      r(TestContext(EndPoint("POST", "localhost", "file.html")))
      result should be(true)
    }

    "route DELETE method" in {
      var result = false
      val r = Routes({ case DELETE(x) => result = true })
      r(TestContext(EndPoint("DELETE", "localhost", "file.html")))
      result should be(true)
    }

    "route HEAD method" in {
      var result = false
      val r = Routes({ case HEAD(x) => result = true })
      r(TestContext(EndPoint("HEAD", "localhost", "file.html")))
      result should be(true)
    }

    "route CONNECT method" in {
      var result = false
      val r = Routes({ case CONNECT(x) => result = true })
      r(TestContext(EndPoint("CONNECT", "localhost", "file.html")))
      result should be(true)
    }

    "route OPTIONS method" in {
      var result = false
      val r = Routes({ case OPTIONS(x) => result = true })
      r(TestContext(EndPoint("OPTIONS", "localhost", "file.html")))
      result should be(true)
    }

    "route TRACE method" in {
      var result = false
      val r = Routes({ case TRACE(x) => result = true })
      r(TestContext(EndPoint("TRACE", "localhost", "file.html")))
      result should be(true)
    }

    "throw MatchError when there is no match" in {
      var result = false
      val r = Routes({ case TRACE(x) => result = true })

      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "file.html")))
      }
    }

  }

}