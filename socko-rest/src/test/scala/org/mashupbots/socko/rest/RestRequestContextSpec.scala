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

import scala.reflect.runtime.{universe => ru}
import org.mashupbots.socko.events.EndPoint
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.Finders
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.mashupbots.socko.events.ImmutableHttpHeaders

class RestRequestContextSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestRequestContext" must {
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val config = RestConfig("1.0", "http://localhost/api")

    "Correctly set the timeout period in the context" in {
      val ctx = RestRequestContext(EndPoint("GET", "localhost", "/api/path/1234"),
        ImmutableHttpHeaders.empty, SockoEventType.HttpRequest, config.requestTimeoutSeconds)

      ctx.timeoutSeconds must be(config.requestTimeoutSeconds)
      (ctx.timeoutTime.getTime - ctx.startTime.getTime) / 1000 must be(config.requestTimeoutSeconds)
    }

  }

}
