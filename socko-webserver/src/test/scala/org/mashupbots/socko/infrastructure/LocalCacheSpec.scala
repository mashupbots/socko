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
package org.mashupbots.socko.infrastructure

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import java.util.Date
import java.util.GregorianCalendar
import java.net.InetSocketAddress
import org.scalatest.GivenWhenThen
import akka.util.duration._

@RunWith(classOf[JUnitRunner])
class LocalCacheSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  "LocalCacheSpec" should {

    val cache = new LocalCache()

    "Store value" in {
      cache.set("key1", "value1")

      val v = cache.get("key1")
      v.isDefined should be(true)
      v.get should equal("value1")

      cache.remove("key1")
      val v2 = cache.get("key1")
      v2.isEmpty should be(true)
    }

    "Expire values" in {
      cache.set("key2", "value1", 10)

      Thread.sleep(50)

      val v = cache.get("key2")
      v.isEmpty should be(true)
    }
  }
}




