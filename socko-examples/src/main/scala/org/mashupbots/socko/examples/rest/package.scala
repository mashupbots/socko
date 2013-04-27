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
package org.mashupbots.socko.examples

/**
 * The example is a clone of the standard Swagger pet store demo app and illustrates how to 
 * utilize the REST handler.
 * 
 * Three examples of how to organize your processor actors are provided
 *  - `Pet` uses a single actor for all operations. The actor is instanced and terminated 
 *     for each request.    
 *  - `Store` uses an actor per operation. The actors are instanced and terminated 
 *     for each request.    
 *  - `User` uses a single actor for all operation. A pool of actors are instanced under
 *     a router at startup. The actors are not terminated; rather they are reused.    
 */
package org.mashupbots.socko.examples.rest {
}