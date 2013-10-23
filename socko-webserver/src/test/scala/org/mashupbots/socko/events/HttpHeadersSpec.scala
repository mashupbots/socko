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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec

class HttpHeadersSpec extends WordSpec with ShouldMatchers {

  "ImmutableHttpHeaders" should {
    "be empty when empty" in {
      val h = ImmutableHttpHeaders.empty
      h.isEmpty should be (true)
      h.size should be (0)
    }

    "be able to retrieve values" in {
      val h = ImmutableHttpHeaders(Seq(("A", "1"), ("a", "11"), ("B", "2"), ("C", "3")))
      h.isEmpty should be (false)
      h.size should be (4)
            
      h.get("a") should be (Some("1"))
      h.getOrElse("a", "") should be ("1")
      h.getOrElse("z", "") should be ("")
      
      val v = h.getAll("A")
      v.size should be (2)
      v(0) should be ("1")
      v(1) should be ("11")
      
      h.contains("b") should be (true)
      h.contains("C") should be (true)
    }
  }
  
  "MutableHttpHeaders" should {
    "be empty when empty" in {
      val h = MutableHttpHeaders()
      h.isEmpty should be (true)
      h.size should be (0)
    }

    "be able to read values" in {
      val h = MutableHttpHeaders(Seq(("A", "1"), ("a", "11"), ("B", "2"), ("C", "3")))
      h.isEmpty should be (false)
      h.size should be (4)
            
      h.get("a") should be (Some("1"))
      h.getOrElse("a", "") should be ("1")
      h.getOrElse("z", "") should be ("")
      
      val v = h.getAll("A")
      v.size should be (2)
      v(0) should be ("1")
      v(1) should be ("11")
      
      h.contains("b") should be (true)
      h.contains("C") should be (true)
    }
    
    "be able to write values" in {
      val h1 = ImmutableHttpHeaders(Seq(("A", "1"), ("a", "11"), ("B", "2"), ("C", "3")))
      val h = MutableHttpHeaders(h1)
      h.isEmpty should be (false)
      h.size should be (4)

      // Replace all instances of "a"
      h.put("a", "111")
      h.get("a") should be (Some("111"))
      h.size should be (3)
      
      h.get("B") should be (Some("2"))
      h.get("c") should be (Some("3"))

      // Append
      h.append("A", "1111")
      h.get("a") should be (Some("111"))	// Still get the 1st occurrence
      h.last.name should be ("A")			// Last instance should be what we've just appended
      h.last.value should be ("1111")
      h.size should be (4)
      
      // Remove
      h.remove("b")
      h.contains("B") should be (false)
      h.size should be (3)
      
    }
    
  }
  
}