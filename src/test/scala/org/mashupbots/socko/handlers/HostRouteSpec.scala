//
// Copyright 2012 Vibul Imtarnasan and David Bolton.
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
package org.mashupbots.socko.handlers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.mashupbots.socko.context.EndPoint
import org.scalatest.GivenWhenThen

@RunWith(classOf[JUnitRunner])
class HostRouteSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  "Routes on the request host" should {
    "route on exact host match" in {
      var result = ""
      val r = Routes({
        case ctx @ PUT(Host("www.abc.com")) => result = "1"
        case Host("www.abc.com") => result = "2"
      })

      when("mixing method and host extractors")
      r(TestContext(EndPoint("GET", "www.abc.com", "/testpath")))
      result should equal("2")

      when("there is an exact host match")
      r(TestContext(EndPoint("PUT", "www.abc.com", "/testpath")))
      result should equal("1")
    }

    "route on host segments" in {
      var result = ""
      var hostSuffix = ""
      val r = Routes({
        case GET(Host(HostSegments(site :: "abc" :: "com" :: Nil))) => {
          result = "1"
        }
        case ctx @ Host(HostSegments(site :: "abc" :: "com" :: Nil)) => {
          // Get storing data in our cache
          ctx.cache.put("site", site)
          result = "2"
        }
        case ctx @ Host(HostSegments("server100" :: x)) => {
          result = "3"
          hostSuffix = x.mkString(".")
        }
      })

      when("there is an exact match with a method")
      r(TestContext(EndPoint("GET", "site1.abc.com", "/record/1")))
      result should equal("1")

      when("there is an exact match without a method")
      var ctx = TestContext(EndPoint("PUT", "site2.abc.com", "/record/100"))
      r(ctx)
      result should equal("2")
      ctx.cache.get("site") should equal(Some("site2"))

      when("the matching patern has a fixed root and variable suffix")
      r(TestContext(EndPoint("PUT", "server100.def.com", "/html/a/b/c/abc.html")))
      result should equal("3")
      hostSuffix should equal("def.com")
    }

    "route on path regular expression" in {
      object MyHostRegex extends HostRegex("""www1\.([a-z]+)\.com""".r)

      var result = ""
      val r = Routes({
        case MyHostRegex(m) => {
          m.group(1) should equal("abc")
          result = "1"
        }
      })

      r(TestContext(EndPoint("GET", "www1.abc.com", "/xxx")))
      result should equal("1")
    }

    "throw MatchError" in {
      var result = ""
      val r = Routes({
        case ctx @ PUT(Host("www.abc.com")) => result = "1"
        case Host("www.abc.com") => result = "2"
        case ctx @ Host(HostSegments(site :: "abc" :: "com" :: Nil)) => result = "3"
      })

      when("no exact match")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "/xxx")))
      }

      when("no case sensitive match")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "www.ABC.com", "/xxx")))
      }

      when("no matching of parts of host not found")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "server1.www.abc.com.au", "/xxx")))
      }
    }

  }
}