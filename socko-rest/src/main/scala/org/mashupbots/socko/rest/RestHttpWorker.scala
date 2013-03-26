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

import java.util.Date
import scala.concurrent.duration._
import org.mashupbots.socko.events.HttpRequestEvent
import akka.actor.Actor
import akka.actor.FSM
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout.durationToTimeout
import org.mashupbots.socko.events.HttpResponseStatus

/**
 * FSM states for [[org.mashupbots.socko.rest.RestHttpWorker]]
 */
sealed trait RestHttpWorkerState

/**
 * FSM data for [[org.mashupbots.socko.rest.RestHttpWorker]]
 */
trait RestHttpWorkerData

/**
 * Processes a HTTP REST request.
 *
 * Processing steps:
 *  - Locates operation in the `registry` and the actor that will be used to process the request
 *  - Deserailizes the request data
 *  - Sends the request data to the processing actor
 *  - Waits for the response data
 *  - Serializes the response data
 *
 * @param registry Registry to find operations
 * @param httpRequest HTTP request to process
 */
class RestHttpWorker(registry: RestRegistry, httpRequest: HttpRequestEvent) extends Actor
  with FSM[RestHttpWorkerState, RestHttpWorkerData] with akka.actor.ActorLogging {

  import context.dispatcher

  //*******************************************************************************************************************
  // Message
  //*******************************************************************************************************************
  private case class Start()

  private case class ProcessingError(reason: Throwable)

  //*******************************************************************************************************************
  // State
  //*******************************************************************************************************************
  /**
   * Locate processing actor, deserialize request and send it to the actor
   */
  case object DispatchRequest extends RestHttpWorkerState

  /**
   * Wait for response to arrive from the actor; and when it does, serialize it back to the caller
   */
  case object WaitingForResponse extends RestHttpWorkerState

  //*******************************************************************************************************************
  // Data
  //*******************************************************************************************************************
  /**
   * Processing data
   */
  case class Data(op: Option[RestOperation] = None, startedOn: Date = new Date()) extends RestHttpWorkerData {
    
    def duration: Long = {
      new Date().getTime - startedOn.getTime
    }
  }

  //*******************************************************************************************************************
  // Transitions
  //*******************************************************************************************************************
  startWith(DispatchRequest, Data())

  when(DispatchRequest) {
    case Event(msg: Start, data: Data) =>
      log.debug(s"RestHttpWorker start")
      val op = registry.findOperation(httpRequest.endPoint)
      val actorPath = registry.findPrcoessingActorPath(op)

      val processingActor = context.actorFor(actorPath)
      if (processingActor.isTerminated) {
        throw RestBindingException(s"Processing actor '${actorPath}' is terminated")
      }

      val restRequest = op.deserializer.deserialize(httpRequest)
      val future = ask(processingActor, restRequest)(registry.config.requestTimeoutSeconds seconds).mapTo[RestResponse]
      future pipeTo self

      goto(WaitingForResponse) using Data(op = Some(op))

    case Event(msg: ProcessingError, data: Data) =>
      stop(FSM.Failure(msg.reason))
      
    case unknown =>
      log.debug("Received unknown message while DispatchRequest: {}", unknown.toString)
      stay
  }

  when(WaitingForResponse) {
    case Event(response: RestResponse, data: Data) =>
      data.op.get.serializer.serialize(httpRequest, response)
      stop(FSM.Normal)

    case Event(msg: akka.actor.Status.Failure, data: Data) =>
      stop(FSM.Failure((msg.cause)))

    case unknown =>
      log.debug("Received unknown message while WaitingForResponse: {}", unknown.toString)
      stay
  }

  onTermination {
    case StopEvent(FSM.Normal, state, data: Data) =>
      log.debug(s"Finished in ${data.duration}ms")
    case StopEvent(FSM.Failure(cause: Throwable), state, data: Data) =>
      httpRequest.response.write(HttpResponseStatus(500))
      log.error(cause, s"Failed with error: ${cause.getMessage}")
    case e: Any =>
      log.debug(s"Shutdown " + e)
  }

  //*******************************************************************************************************************
  // Boot up
  //*******************************************************************************************************************
  /**
   * Kick start processing with a message to ourself
   */
  override def preStart {
    self ! Start()
  }

  /**
   * Start with a ProcessingError so that we record the unhandled exception during processing and stop
   */
  override def postRestart(reason: Throwable) {
   self ! ProcessingError(reason)
  }

}

