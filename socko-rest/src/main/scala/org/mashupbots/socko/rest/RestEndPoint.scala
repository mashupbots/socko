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
 * A REST end point encapsulates the meta data that describes a REST operation along
 * with its associated request and response data.
 * 
 * At runtime, the end point binds incoming data to a request object and dispatches
 * it for processing for the specified actor.  The actor must return the specified
 * response object which the RestEndPoint will serialize and return to the caller.
 */
case class RestEndPoint(
    operation: RestOperation,
    requestClass: ru.ClassSymbol,
    responseClass: ru.ClassSymbol) {

}

