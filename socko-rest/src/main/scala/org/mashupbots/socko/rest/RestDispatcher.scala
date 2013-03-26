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

import akka.actor.ActorRef
import akka.actor.ActorSystem

/**
 * Instances or finds an actor to process a [[org.mashupbots.socko.rest.RestRequest]].
 */
trait RestDispatcher {

  /**
   * Returns an actor to which `request` will be sent for processing
   * 
   * @param actorSystem Actor system in which new actors maybe created
   * @param request Rest Request to dispatch
   * @return `ActorRef` of actor to which `request` will be sent for processing
   */
  def getActor(actorSystem: ActorSystem, request: RestRequest): ActorRef
}

