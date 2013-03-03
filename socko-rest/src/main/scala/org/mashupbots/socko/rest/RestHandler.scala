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
package org.mashupbots.socko.rest

import akka.actor.Actor
import akka.event.Logging
import org.mashupbots.socko.events.HttpRequestEvent

class RestHandler(config: RestHandlerConfig) extends Actor {
   
  /**
   * 
   */
  def receive = {
    case request: HttpRequestEvent => {
      
    }
  }  
}

/**
 * Configuration for the REST handler
 * 
 * @param apiVersion the version of your API
 * @param swaggerVersion Swagger definition version
 * @param basePath Full base path to your API from an external caller's point of view
 * @param actorPathMap Optional map of key/actor path. The key is specified in REST operation
 *   `actorPath` that are prefixed with `lookup:`. For example,
 *    {{{
 *    // Uses lookup
 *    @Get(uriTemplate = "/pets", actorPath = "lookup:mykey")
 * 
 *    // Will NOT use lookup
 *    @Get(uriTemplate = "/pets", actorPath = "/my/actor/path")
 *    }}} 
 *   
 */
case class RestHandlerConfig(
  apiVersion: String,
  swaggerVersion: SwaggerVersion.Value,
  basePath: String,
  actorPathMap: Map[String, String] = Map.empty)
 
/**
 * Support swagger version
 */
object SwaggerVersion extends Enumeration {
  type SwaggerVersion = Value
  val V1_1 = Value("1.1")
}