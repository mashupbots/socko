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

import org.mashupbots.socko.events.EndPoint
import java.util.UUID

/**
 * Context of the rest request
 *
 * @param id UUID for this rest request/response pair
 * @param endPoint HTTP URL at which the request was received
 * @param headers HTTP request headers
 */
case class RestRequestContext(
  id: UUID,
  endPoint: EndPoint,
  headers: Map[String, String]) {

  /**
   * Constructor that automatically adds a UUId for us
   */
  def this(endPoint: EndPoint,
    headers: Map[String, String]) =
    this(UUID.randomUUID(), endPoint, headers)

}