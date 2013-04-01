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
import org.mashupbots.socko.events.HttpResponseStatus

import akka.actor.Actor
import akka.actor.FSM
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout.durationToTimeout

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
 * @param httpRequestEvent HTTP request to process
 */
class RestHttpWorker(registry: RestRegistry, httpRequestEvent: HttpRequestEvent) extends Actor
  with FSM[RestHttpWorkerState, RestHttpWorkerData] with akka.actor.ActorLogging {

  import context.dispatcher

  private val cfg = registry.config

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
  case class Data(
    op: Option[RestOperation] = None,
    req: Option[RestRequest] = None,
    startedOn: Date = new Date()) extends RestHttpWorkerData {

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
      val op = registry.findOperation(httpRequestEvent.endPoint)
      val restRequest = op.deserializer.deserialize(httpRequestEvent)

      val processingActor = op.dispatcher.getActor(context.system, restRequest)
      if (processingActor.isTerminated) {
        throw RestProcessingException(s"Processing actor '${processingActor.path}' for '${op.deserializer.requestClass.fullName}' is terminated")
      }

      if (op.definition.accessSockoEvent) {
        RestRequestEvents.put(restRequest.context, httpRequestEvent)
      }

      val future = ask(processingActor, restRequest)(cfg.requestTimeoutSeconds seconds).mapTo[RestResponse]
      future pipeTo self

      goto(WaitingForResponse) using Data(op = Some(op), req = Some(restRequest))

    case Event(msg: ProcessingError, data: Data) =>
      stop(FSM.Failure(msg.reason))

    case unknown =>
      log.debug("Received unknown message while DispatchRequest: {}", unknown.toString)
      stay
  }

  when(WaitingForResponse) {
    case Event(response: RestResponse, data: Data) =>
      data.op.get.serializer.serialize(httpRequestEvent, response)
      stop(FSM.Normal)

    case Event(msg: akka.actor.Status.Failure, data: Data) =>
      stop(FSM.Failure((msg.cause)))

    case unknown =>
      log.debug("Received unknown message while WaitingForResponse: {}", unknown.toString)
      stay
  }

  onTermination {
    case StopEvent(FSM.Normal, state, data: Data) =>
      clearSockoEventCache(data)
      log.debug(s"Finished in ${data.duration}ms")

    case StopEvent(FSM.Failure(cause: Throwable), state, data: Data) =>
      clearSockoEventCache(data)
      val isHead = httpRequestEvent.endPoint.isHEAD
      cause match {
        case _: RestBindingException =>
          val msg = if (!isHead && cfg.reportOn400BadRequests) cause.getMessage else ""
          httpRequestEvent.response.write(HttpResponseStatus(400), msg)
        case _: Throwable =>
          val msg = if (!isHead && cfg.reportOn500InternalServerError) cause.getMessage else ""
          httpRequestEvent.response.write(HttpResponseStatus(500), msg)
      }

      log.error(cause, s"Failed with error: ${cause.getMessage}")

    case e: Any =>
      log.debug(s"Shutdown " + e)
  }

  private def clearSockoEventCache(data: Data) = {
    try {
      if (data.op.isDefined && data.req.isDefined) {
        if (data.op.get.definition.accessSockoEvent) {
          RestRequestEvents.remove(data.req.get.context)
        }
      }
    } catch {
      // Can ignore the error because the entry should be automatically removed
      // by the LocalCache.
      case ex: Throwable => log.error(ex, "Error clearing SockoEvent")
    }
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

} // end class

/**
 * FSM states for [[org.mashupbots.socko.rest.RestHttpWorker]]
 */
sealed trait RestHttpWorkerState

/**
 * FSM data for [[org.mashupbots.socko.rest.RestHttpWorker]]
 */
trait RestHttpWorkerData