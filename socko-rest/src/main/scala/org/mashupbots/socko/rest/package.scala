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
package org.mashupbots.socko

/**
 * REST API processor for Socko
 * 
 * 
 * All HTTP operations for a particular path should be grouped in a single api object. There should be no duplicate HTTP methods for a single path.
 * @Get
 * @Post
 * @Put
 * @Delete
 * 
 * @QueryString
 * @Path
 * @Header
 * @Body
 * 
 * @RestGet(
 * 	Name="",		//Nickname
 * 	UriTemplate="/user/{id}",
 * 	Description="",
 * 	Notes="",
 * 	Actor="",
 * 	Deprecated = "",
 * 	ResponseClass="",  // Default to GetUserResponse by convention, void, primitive, complex
 * 	ErrorResponses="Code=Reason,Code=Reason"
 * )
 * case class GetUserRequest(ctx:RequestContext, 
 * 	@Path(Name="default to val name", Description="", Required="", AllowableValues="") id:String
 * )
 * 
 * case class GetUserResponse(ctx:ResponseContext,
 * 	User user
 * )
 * 
 */
package org.mashupbots.socko.rest {
  
}