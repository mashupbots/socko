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

import scala.reflect.runtime.{ universe => ru }
import org.json4s._
import org.json4s.native.{ JsonMethods => jsonMethods }
import org.json4s.native.{ Serialization => json }
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.mashupbots.socko.infrastructure.ReflectUtil
import org.mashupbots.socko.infrastructure.CharsetUtil

class RestApiDocsSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestApiDocs" must {

    val cfg = RestConfig("1.0", "/api")
    val registry = RestRegistry("org.mashupbots.socko.rest.petshop", cfg)
    
    "correctly produce resource listing" in {
      val resourceListing = registry.apiDocs("/api-docs.json")
      val resourceListingDoc = """
        {
        	"apiVersion":"1.0",
        	"swaggerVersion":"1.1",
        	"basePath":"/api",
        	"apis":[
        		{
        			"path":"/api-docs.json/pet",
        			"description":""
        		},
        		{
        			"path":"/api-docs.json/store",
        			"description":""
        		},
        		{
        			"path":"/api-docs.json/user",
        			"description":""
        		}
        	]
        }
        """
      log.debug("ResourceListing=" + pettyJson(new String(resourceListing, CharsetUtil.UTF_8)))
      new String(resourceListing, CharsetUtil.UTF_8) must be(compactJson(resourceListingDoc))
    }

    "correctly produce pets API declaration" in {
      val api = registry.apiDocs("/api-docs.json/pet")
      val apiDoc = """

        """
      log.debug("Pets API declaration=" + pettyJson(new String(api, CharsetUtil.UTF_8)))
      new String(api, CharsetUtil.UTF_8) must be(compactJson(apiDoc))
    }

    def pettyJson(json: String): String = {
      jsonMethods.pretty(jsonMethods.render(jsonMethods.parse(json, useBigDecimalForDouble = true)))
    }
    
    def compactJson(json: String): String = {
      jsonMethods.compact(jsonMethods.render(jsonMethods.parse(json, useBigDecimalForDouble = true)))
    }
  }
}


