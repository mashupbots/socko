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
package org.mashupbots.socko.processors

import java.io.File
import java.util.Date

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec

@RunWith(classOf[JUnitRunner])
class StaticFileLastModifiedCacheSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  "StaticFileLastModifiedCache" should {

    "correctly get a file's last modified date" in {
      val aFile = File.createTempFile("StaticFileLastModifiedCache", ".txt");
      aFile.deleteOnExit()
      val aFileName = aFile.getCanonicalPath
      val out = new java.io.FileWriter(aFile)
      out.write("test")
      out.close

      when("file not cached")
      StaticFileLastModifiedCache.get(new File(aFileName), 2) should equal(aFile.lastModified)

      when("file is cached")
      val cachedValue = StaticFileLastModifiedCache.get(new File(aFileName), 2)
      cachedValue should equal(aFile.lastModified)

      when("file is updated but cache has not timed out")
      aFile.setLastModified(new Date().getTime - 5000)
      StaticFileLastModifiedCache.get(new File(aFileName), 2) should equal(cachedValue)

      when("cache timed out, new value should be returned")
      Thread.sleep(2000);
      StaticFileLastModifiedCache.get(new File(aFileName), 2) should equal(aFile.lastModified)
    }

    "throw Exception if file is null" in {
      val ex = intercept[IllegalArgumentException] {
        StaticFileLastModifiedCache.get(null, 3)
      }
    }

    "return 0 if file is not found" in {
      StaticFileLastModifiedCache.get(new File("/tmp/notexist"), 3) should equal(0)
    }

  }
}