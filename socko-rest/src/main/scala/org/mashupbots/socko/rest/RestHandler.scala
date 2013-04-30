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

import java.util.UUID

import scala.concurrent.duration._

import org.mashupbots.socko.events.HttpRequestEvent

import akka.actor.Actor
import akka.actor.FSM
import akka.actor.Props
import akka.actor.Terminated

/**
 * The initial processing point for incoming requests. It farms requests out to worker.
 *
 * Capacity control is implemented here. The maximum number of workers is limited. When the limit
 * is reached, messages are rescheduled for processing.
 *
 * @param registry Registry to find operations
 */
class RestHandler(registry: RestRegistry) extends Actor with FSM[RestHandlerState, RestHandlerData] with akka.actor.ActorLogging {

  import context.dispatcher

  //*******************************************************************************************************************
  // State
  //*******************************************************************************************************************
  /**
   * Workers available to process request
   */
  case object Active extends RestHandlerState

  /**
   * All workers currently being utilized
   */
  case object MaxCapacity extends RestHandlerState

  //*******************************************************************************************************************
  // Data
  //*******************************************************************************************************************
  /**
   * Data used when active
   *
   * @param workerCount Number of [[org.mashupbots.socko.rest.RestHttpWorker]]s running.
   */
  case class Data(workerCount: Int) extends RestHandlerData {

    def incrementWokerCount(): Data = {
      this.copy(workerCount = workerCount + 1)
    }

    def decrementWokerCount(): Data = {
      if (workerCount > 0) this.copy(workerCount = workerCount - 1)
      else this.copy(workerCount = 0)
    }

  }

  //*******************************************************************************************************************
  // Transitions
  //*******************************************************************************************************************
  startWith(Active, Data(0))

  when(Active) {
    case Event(msg: HttpRequestEvent, data: Data) =>
      // Start worker and register death watch so that we will receive a `Terminated` message when the actor stops
      val worker = context.actorOf(Props(new RestHttpWorker(registry, msg)), "worker-" + UUID.randomUUID().toString)
      context.watch(worker)      
      
      // Manage worker count
      val newData = data.incrementWokerCount()
      if (newData.workerCount < registry.config.maxWorkerCount) stay using newData
      else goto(MaxCapacity) using newData

    case Event(msg: Terminated, data: Data) =>
      // A worker has terminated so reduce the count
      stay using data.decrementWokerCount()
      
    case Event(msg: RestHandlerWorkerCountRequest, data: Data) =>
      sender ! data.workerCount
      stay

    case unknown => 
      log.debug("Received unknown message while Active: {}", unknown.toString)
      stay
      
  }

  when(MaxCapacity) {
    case Event(msg: HttpRequestEvent, data: Data) =>
      log.info("Rescheduling HttpRequestEvent channel {} because all workers are busy.", msg.channel.getId)
      context.system.scheduler.scheduleOnce(registry.config.maxWorkerRescheduleMilliSeconds.milliseconds, self, msg)
      stay
      
    case Event(msg: Terminated, data: Data) =>
      // A worker has terminated so reduce the count
      goto(Active) using data.decrementWokerCount()
      
    case Event(msg: RestHandlerWorkerCountRequest, data: Data) =>
      sender ! data.workerCount
      stay

    case unknown => 
      log.debug("Received unknown message while MaxCapacity: {}", unknown.toString)
      stay      
  }

}	// end class


/**
 * FSM states for [[org.mashupbots.socko.rest.RestHandler]]
 */
sealed trait RestHandlerState

/**
 * FSM data for [[org.mashupbots.socko.rest.RestHandler]]
 */
trait RestHandlerData

/**
 * Message that can be sent to a RestHandler to retrieve the current number of workers
 */
case class RestHandlerWorkerCountRequest()