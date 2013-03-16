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
package org.mashupbots.socko.rest.test2

import org.mashupbots.socko.rest.RestGet
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

@RestGet(
  uriTemplate = "/pets",
  actorPath = "/my/actor/path")
case class GetPets1Request(context: RestRequestContext) extends RestRequest {

}

case class GetPets1Response(context: RestResponseContext) extends RestResponse {

}

// A duplicate of the above end point address
@RestGet(
  uriTemplate = "/pets",
  actorPath = "/my/actor/path")
case class GetPets2Request(context: RestRequestContext) extends RestRequest {

}

case class GetPets2Response(context: RestResponseContext) extends RestResponse {

}

