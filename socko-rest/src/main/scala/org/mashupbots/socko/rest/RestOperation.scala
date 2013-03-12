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
import org.mashupbots.socko.infrastructure.Logger

/**
 * A REST operation processes data in the following manner:
 *  - takes input data, deserializes it into a [[org.mashupbots.socko.rest.RestRequest]]
 *  - dispatches the [[org.mashupbots.socko.rest.RestRequest]] to an actor for processing
 *  - actor returns a [[org.mashupbots.socko.rest.RestResponse]]
 *  - serializes [[org.mashupbots.socko.rest.RestResponse]] and returns the result to the caller
 *
 * @param definition Meta data describing the object
 * @param requestClass The [[org.mashupbots.socko.rest.RestRequest]] class used to store deserialized inputs
 * @param responseClass The [[org.mashupbots.socko.rest.RestResponse]] class used to store outputs to be serialized
 */
case class RestOperation(
  definition: RestOperationDef,
  requestClass: ru.ClassSymbol,
  responseClass: ru.ClassSymbol) {

}

