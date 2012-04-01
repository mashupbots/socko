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
class PathRouteSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  "Routes on the request path" should {
    "route on exact path match" in {
      var result = ""
      val r = Routes({
        case PUT(Path("/testpath")) => result = "1"
        case Path("/testpath/folder1") => result = "2"
        case Path("/testpath") => result = "3"
      })

      r(TestContext(EndPoint("GET", "localhost", "/testpath")))
      result should equal("3")

      r(TestContext(EndPoint("PUT", "localhost", "/testpath")))
      result should equal("1")
    }

    "route on path segments" in {
      var result = ""
      var relativePath = ""
      val r = Routes({
        case ctx @ GET(Path(PathSegments("record" :: id :: Nil))) => {
          result = "1"
        }
        case ctx @ Path(PathSegments("record" :: id :: Nil)) => {
          ctx.cache.put("id", id) // test cache storage
          result = "2"
        }
        case ctx @ Path(PathSegments("html" :: x)) => {
          relativePath = x.mkString("/", "/", "")
          result = "3"
        }
      })

      when("there is an exact match with a method")
      r(TestContext(EndPoint("GET", "localhost", "/record/1")))
      result should equal("1")

      when("there is an exact match without a method")
      var ctx = TestContext(EndPoint("PUT", "localhost", "/record/100"))
      r(ctx)
      result should equal("2")
      ctx.cache.get("id") should equal(Some("100"))

      when("the matching patern has a fixed root and variable subfolders")
      r(TestContext(EndPoint("PUT", "localhost", "/html/a/b/c/abc.html")))
      result should equal("3")
      relativePath should equal("/a/b/c/abc.html")
    }

    "route on path regular expression" in {
      object MyPathRegex extends PathRegex("""/path/([a-z0-9]+)/([a-z0-9]+)""".r)

      var result = ""
      val r = Routes({
        case MyPathRegex(m) => {
          m.group(1) should equal("to")
          m.group(2) should equal("file")
          result = "1"
        }
      })

      r(TestContext(EndPoint("PUT", "www.def.com", "/path/to/file")))
      result should equal("1")
    }

    "throw MatchError" in {
      var result = ""
      val r = Routes({
        case PUT(Path("/testpath")) => result = "1"
        case Path("/testpath/folder1") => result = "2"
        case Path("/testpath") => result = "3"
        case Path(PathSegments("html" :: x)) => result = "4"
      })

      when("no exact match")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "/nopathmatch")))
      }

      when("no case sensitive match")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "/TestPath")))
      }

      when("no matching of deeper path not found")
      intercept[MatchError] {
        r(TestContext(EndPoint("GET", "localhost", "/testpath/folder1/file.html")))
      }
    }

  }
}