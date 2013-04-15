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

class RestEndPointSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestEndPointSpec" should {

    "compare endpoint address with different methods" in {
      RestEndPoint("GET", "/api", "/pets").comparePath(
        RestEndPoint("PUT", "/api", "/pets")) must be(false)

      RestEndPoint("PUT", "/api", "/pets/{id}").comparePath(
        RestEndPoint("POST", "/api", "/pets/{id}")) must be(false)

      RestEndPoint("PUT", "/api", "/pets/{id}").comparePath(
        RestEndPoint("POST", "/api", "/{type}/{id}")) must be(false)
    }

    "compare endpoint address with different paths" in {
      // Same
      RestEndPoint("GET", "/api", "/pets").comparePath(
        RestEndPoint("GET", "/api", "/pets")) must be(true)

      // Different - static path segment names pets vs dogs
      RestEndPoint("GET", "/api", "/pets").comparePath(
        RestEndPoint("GET", "/api", "/dogs")) must be(false)

      // Different - number of segments 1 vs 2
      RestEndPoint("GET", "/api", "/pets").comparePath(
        RestEndPoint("GET", "/api", "/pets/dogs")) must be(false)

      // Same
      RestEndPoint("PUT", "/api", "/pets/{id}").comparePath(
        RestEndPoint("PUT", "/api", "/pets/{id}")) must be(true)

      // Different - Mix of variable and static makes
      RestEndPoint("PUT", "/api", "/pets/{id}").comparePath(
        RestEndPoint("PUT", "/api", "/{type}/{id}")) must be(false)

      RestEndPoint("PUT", "/api", "/{type}/{id}").comparePath(
        RestEndPoint("PUT", "/api", "/pets/{id}")) must be(false)

    }

  }
}