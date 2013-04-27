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

import org.mashupbots.socko.events.HttpResponseStatus

/**
 * Context of the REST response. Contains response meta-data.
 *
 * @param requestContext The request context to which this is a response
 * @param status HTTP status
 * @param headers HTTP response headers
 */
case class RestResponseContext(
  requestContext: RestRequestContext,
  status: HttpResponseStatus,
  headers: Map[String, String]) {

  /**
   * Alternative constructor where you need only specify the request
   * 
   * @param request The request to which this is a response
   * @param status HTTP status
   * @param headers HTTP response headers
   */
  def this(request: RestRequest,
    status: HttpResponseStatus,
    headers: Map[String, String]) =
    this(request.context, status, headers)

  /**
   * Alternative constructor where you need only specify the request.
   * The status is set to 200 and no custom response headers are set.
   * 
   * @param request The request to which this is a response
   */
  def this(request: RestRequest) =
    this(request.context, HttpResponseStatus(200), Map.empty[String, String])

}