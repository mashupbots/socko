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
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec

@RunWith(classOf[JUnitRunner])
class ConcatenateRouteSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  "Route concatenation" should {
    "route on exact match" in {
      var result = ""
      val r = Routes({
        case ctx @ PUT(_) & Host("www.abc.com") & Path("/testpath") => result = "1"
        case ctx @ GET(Path("/testpath")) & QueryString("action=save") => result = "2"
      })

      r(TestContext(EndPoint("PUT", "www.abc.com", "/testpath")))
      result should equal("1")
      
      r(TestContext(EndPoint("GET", "www.def.com", "/testpath?action=save")))
      result should equal("2")      
    }

    "throw MatchError when there is no match" in {
      var result = ""
      val r = Routes({
        case ctx @ PUT(_) & Host("www.abc.com") & Path("/testpath") => result = "1"
      })

      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "www.abc.com", "/testpath")))
      }
      intercept[MatchError] {
        r(TestContext(EndPoint("PUT", "www.def.com", "/testpath")))
      }
      intercept[MatchError] {
        r(TestContext(EndPoint("PUT", "www.abc.com", "/testpath1")))
      }
      
    }

  }
}