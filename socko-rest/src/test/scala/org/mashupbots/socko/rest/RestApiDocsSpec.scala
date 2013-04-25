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
    
    "Identify swagger types" in {
      SwaggerReflector.dataType(ru.typeOf[String]) must be ("string")
      SwaggerReflector.dataType(ru.typeOf[Int]) must be ("int")
      
      SwaggerReflector.dataType(ru.typeOf[List[Int]]) must be ("List[int]")
      SwaggerReflector.dataType(ru.typeOf[Array[String]]) must be ("Array[string]")
      SwaggerReflector.dataType(ru.typeOf[Set[Float]]) must be ("Set[float]")
     
      SwaggerReflector.dataType(ru.typeOf[Cow]) must be ("Cow")
      SwaggerReflector.dataType(ru.typeOf[Option[Cow]]) must be ("Cow")
      SwaggerReflector.dataType(ru.typeOf[List[Cow]]) must be ("List[Cow]")
      SwaggerReflector.dataType(ru.typeOf[Array[Cow]]) must be ("Array[Cow]")
      SwaggerReflector.dataType(ru.typeOf[Set[Cow]]) must be ("Set[Cow]")
    }
    
    "correctly produce resource listing" in {
      val resourceListing = registry.swaggerApiDocs("/api-docs.json")
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
      val api = registry.swaggerApiDocs("/api-docs.json/pet")
      val apiDoc = """
		{
		  "apiVersion":"1.0",
		  "swaggerVersion":"1.1",
		  "basePath":"/api",
		  "resourcePath":"/pet",
		  "apis":[{
		    "path":"/pet",
		    "operations":[{
		      "httpMethod":"POST",
		      "summary":"Add a new pet to the store",
		      "responseClass":"void",
		      "nickname":"addPet",
		      "parameters":[{
		        "name":"pet",
		        "description":"Pet object that needs to be added to the store",
		        "paramType":"body",
		        "dataType":"Pet",
		        "required":true
		      }],
		      "errorResponses":[{
		        "code":405,
		        "reason":"Invalid input"
		      }]
		    },{
		      "httpMethod":"PUT",
		      "summary":"Update an existing pet",
		      "responseClass":"void",
		      "nickname":"updatePet",
		      "parameters":[{
		        "name":"pet",
		        "description":"Pet object that needs to be updated in the store",
		        "paramType":"body",
		        "dataType":"Pet",
		        "required":true
		      }],
		      "errorResponses":[{
		        "code":400,
		        "reason":"Invalid ID supplied"
		      },{
		        "code":404,
		        "reason":"Pet not found"
		      },{
		        "code":405,
		        "reason":"Validation exception"
		      }]
		    }]
		  },{
		    "path":"/pet/findByStatus",
		    "operations":[{
		      "httpMethod":"GET",
		      "summary":"Finds Pets by status",
		      "notes":"Multiple status values can be provided with comma seperated strings",
		      "responseClass":"List[Pet]",
		      "nickname":"findPetsByStatus",
		      "parameters":[{
		        "name":"status",
		        "description":"Status values that need to be considered for filter",
		        "paramType":"query",
		        "dataType":"string",
		        "required":true,
		        "allowableValues":{
		          "values":["available","pending","sold"],
		          "valueType":"LIST"
		        },
		        "allowMultiple":true
		      }],
		      "errorResponses":[{
		        "code":405,
		        "reason":"Invalid status value"
		      }]
		    }]
		  },{
		    "path":"/pet/findPetsByTags",
		    "operations":[{
		      "httpMethod":"GET",
		      "summary":"Finds Pets by tags",
		      "notes":"Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing.",
		      "deprecated":true,
		      "responseClass":"List[Pet]",
		      "nickname":"findPetsByTags",
		      "parameters":[{
		        "name":"tags",
		        "description":"Tags to filter by",
		        "paramType":"query",
		        "dataType":"string",
		        "required":true
		      }],
		      "errorResponses":[{
		        "code":405,
		        "reason":"Invalid tag value"
		      }]
		    }]
		  },{
		    "path":"/pet/{petId}",
		    "operations":[{
		      "httpMethod":"GET",
		      "summary":"Find pet by ID",
		      "notes":"Returns a pet based on ID",
		      "responseClass":"Pet",
		      "nickname":"getPetById",
		      "parameters":[{
		        "name":"petId",
		        "description":"ID of pet that needs to be fetched",
		        "paramType":"path",
		        "dataType":"string",
		        "required":true
		      }],
		      "errorResponses":[{
		        "code":400,
		        "reason":"Invalid ID supplied"
		      },{
		        "code":404,
		        "reason":"Pet not found"
		      }]
		    }]
		  }],
		  "models":{
		    "Tag":{
		      "id":"Tag",
		      "properties":{
		        "name":{
		          "type":"string",
		          "required":true
		        },
		        "id":{
		          "type":"long",
		          "required":true
		        }
		      }
		    },
		    "Category":{
		      "id":"Category",
		      "properties":{
		        "name":{
		          "type":"string",
		          "required":true
		        },
		        "id":{
		          "type":"long",
		          "required":true
		        }
		      }
		    },
		    "Pet":{
		      "id":"Pet",
		      "properties":{
		        "name":{
		          "type":"string",
		          "required":true
		        },
		        "tags":{
		          "type":"Array",
		          "required":true,
		          "items":{
		            "$ref":"Tag"
		          }
		        },
		        "photoUrls":{
		          "type":"Array",
		          "required":true,
		          "items":{
		            "type":"string"
		          }
		        },
		        "id":{
		          "type":"long",
		          "required":true
		        },
		        "status":{
		          "type":"string",
                  "description":"pet status in the store",
		          "required":true,
                  "allowableValues":{
			        "values":["available","pending","sold"],
			        "valueType":"LIST"
			      }
		        },
		        "category":{
		          "type":"Category",
		          "required":true
		        }
		      }
		    }
		  }
		}
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


case class Cow(moo: String)
