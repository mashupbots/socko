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
  // Messages
  //*******************************************************************************************************************
  private case class Start()
  private case class ProcessError(reason: Throwable)

  //*******************************************************************************************************************
  // State
  //*******************************************************************************************************************
  /**
   * Locate processing actor, deserialize request and send it to the actor
   */
  case object DispatchingRequest extends RestHttpWorkerState

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
  startWith(DispatchingRequest, Data())

  when(DispatchingRequest) {
    case Event(msg: Start, data: Data) =>
      log.debug(s"RestHttpWorker start")

      // Find operation
      val op = registry.findOperation(httpRequestEvent.endPoint)
      if (op.isEmpty) {
        // Operation not found. See if this is a request for api-docs 
        // More efficient to do this after operation lookup rather than take a hit on every request 
        val docs = registry.swaggerApiDocs.get(httpRequestEvent.endPoint.path)
        if (docs.isDefined) {
          httpRequestEvent.response.write(docs.get, "application/json;charset=UTF-8")
          stop(FSM.Normal)
        } else {
          throw RestNotFoundException(s"Cannot find operation for request to: '${httpRequestEvent.endPoint.method} ${httpRequestEvent.endPoint.path}'")
        }
      } else {
        // Found operation so build request and dispatch
        val op1 = op.get
        val opDeserializer = op1.deserializer
        val restRequest = opDeserializer.deserialize(httpRequestEvent)

        // Get actor
        val processingActor = op1.registration.processorActor(context.system, restRequest)

        // Cache the request event. It will be automatically removed after a period of time (5 seconds)
        if (op1.accessSockoEvent) {
          RestRequestEvents.put(restRequest.context, httpRequestEvent, registry.config.sockoEventCacheTimeoutMilliSeconds)
        }

        if (op1.registration.customSerialization) {
          // Custom serialization so no need to wait for a response to serialize
          processingActor ! restRequest
          stop(FSM.Normal)
        } else {
          // Wait for response to serialize
          val future = ask(processingActor, restRequest)(cfg.requestTimeoutSeconds.seconds).mapTo[RestResponse]
          future pipeTo self
          goto(WaitingForResponse) using Data(op = op, req = Some(restRequest))
        }
      }

    case Event(msg: ProcessError, data: Data) =>
      // Error raised from unhandled exception after this actor restarts
      // We don't want to process again so just stop and record the error
      stop(FSM.Failure(msg.reason))

    case unknown =>
      log.debug("Received unknown message while DispatchingRequest: {}", unknown.toString)
      stay
  }

  when(WaitingForResponse) {
    case Event(response: RestResponse, data: Data) =>
      // We got a response from the actor so serialize and stop
      data.op.get.serializer.serialize(httpRequestEvent, response)
      stop(FSM.Normal)

    case Event(msg: akka.actor.Status.Failure, data: Data) =>
      // Actor error or timed out so stop and report the error
      stop(FSM.Failure((msg.cause)))

    case unknown =>
      log.debug("Received unknown message while WaitingForResponse: {}", unknown.toString)
      stay
  }

  onTermination {
    case StopEvent(FSM.Normal, state, data: Data) =>
      log.debug(s"Finished in ${data.duration}ms")

    case StopEvent(FSM.Failure(cause: Throwable), state, data: Data) =>
      val isHead = httpRequestEvent.endPoint.isHEAD
      cause match {
        case _: RestNotFoundException =>
          httpRequestEvent.response.write(HttpResponseStatus(404))
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
   * Start with a ProcessError so that we record the unhandled exception during processing and stop
   */
  override def postRestart(reason: Throwable) {
    self ! ProcessError(reason)
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