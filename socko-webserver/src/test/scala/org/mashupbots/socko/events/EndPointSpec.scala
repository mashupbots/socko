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
package org.mashupbots.socko.events

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec

@RunWith(classOf[JUnitRunner])
class EndPointSpec extends WordSpec with ShouldMatchers {

  "EndPoint" should {

    "throw exception with null or empty method" in {
      val msg = "EndPoint method cannot be null or empty string"

      val ex = intercept[IllegalArgumentException] {
        val ep = EndPoint(null, "localhost", "/folder/file.html?p1=v1&p2=v2")
      }
      ex.getMessage should include(msg)

      val ex2 = intercept[IllegalArgumentException] {
        val ep = EndPoint("", "localhost", "/folder/file.html?p1=v1&p2=v2")
      }
      ex2.getMessage should include(msg)
    }

    "throw exception with null or empty host" in {
      val msg = "EndPoint host cannot be null or empty string"

      val ex = intercept[IllegalArgumentException] {
        val ep = EndPoint("GET", null, "/folder/file.html?p1=v1&p2=v2")
      }
      ex.getMessage should include(msg)

      val ex2 = intercept[IllegalArgumentException] {
        val ep = EndPoint("GET", "", "/folder/file.html?p1=v1&p2=v2")
      }
      ex2.getMessage should include(msg)
    }

    "throw exception with null or empty uri" in {
      val msg = "EndPoint uri cannot be null or empty string"

      val ex = intercept[IllegalArgumentException] {
        val ep = EndPoint("GET", "localhost", null)
      }
      ex.getMessage should include(msg)

      val ex2 = intercept[IllegalArgumentException] {
        val ep = EndPoint("GET", "localhost", "")
      }
      ex2.getMessage should include(msg)
    }

    "split uri into path and querystring" in {
      val ep = EndPoint("GET", "localhost", "/folder/file.html?p1=v1&p2=v2")

      ep.method should equal("GET")
      ep.host should equal("localhost")
      ep.path should equal("/folder/file.html")
      ep.queryString should equal("p1=v1&p2=v2")
    }

    "handle uri with no querystring" in {
      val ep = EndPoint("GET", "localhost", "/folder/file.html")

      ep.method should equal("GET")
      ep.host should equal("localhost")
      ep.path should equal("/folder/file.html")
      ep.queryString should equal("")
      ep.queryStringMap should be('empty)
    }

    "should convert querystring into a map" in {
      val ep = EndPoint("GET", "localhost", "/folder/file.html?p1=v1&p2=v21&p2=v22")

      ep.queryStringMap should not be ('empty)
      ep.queryStringMap should have size (2)
      ep.queryStringMap("p1").get(0) should be("v1")
      ep.queryStringMap("p2").get(0) should be("v21")
      ep.queryStringMap("p2").get(1) should be("v22")

      ep.getQueryString("p1") should be(Some("v1"))
      ep.getQueryString("p2") should be(Some("v21")) // getQueryString returns only the 1st value

      ep.getQueryString("notexist") should be(None)
    }

  }
}