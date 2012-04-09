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
package org.mashupbots.socko.examples.routes

import scala.collection.JavaConversions.asScalaBuffer
import org.apache.http.HttpHeaders
import org.jboss.netty.channel.ChannelLocal
import org.mashupbots.socko.context.HttpChunkProcessingContext
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.context.WsProcessingContext
import org.mashupbots.socko.postdecoder.InterfaceHttpData.HttpDataType
import org.mashupbots.socko.postdecoder.Attribute
import org.mashupbots.socko.postdecoder.DefaultHttpDataFactory
import org.mashupbots.socko.postdecoder.FileUpload
import org.mashupbots.socko.postdecoder.HttpPostRequestDecoder
import akka.actor.Actor
import akka.event.Logging
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Time processor that returns time in the response
 */
class TimeProcessor extends Actor {
  val log = Logging(context.system, this)

  /**
   * Returns the time in the specified timezone.
   *
   * This actor only receives 1 time of message: `TimeRequest`.
   *
   * The message contains the `HttpRequestProcessingContext` that contains request data and will be used to
   * write the response.
   */
  def receive = {
    case request: TimeRequest =>

      val tz = if (request.timezone.isDefined) {
        val tzid = TimeZone.getAvailableIDs.find(s => 
          s.toLowerCase().contains(request.timezone.get.replace("%20", "_").toLowerCase))
        if (tzid.isDefined) {
          TimeZone.getTimeZone(tzid.get)
        } else {
          TimeZone.getDefault
        }
      } else {
        TimeZone.getDefault
      }

      val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      dateFormatter.setTimeZone(tz)
      val time = new GregorianCalendar()
      val ts = dateFormatter.format(time.getTime())

      request.context.writeResponse("The time is " + ts + ".\nThe timezone is " +
        dateFormatter.getTimeZone.getDisplayName)
      context.stop(self)
    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }
}

/**
 * Request Message
 *
 * @param context HTTP Request context containing request data and context for writing response
 * @param timzone The requested timezone
 */
case class TimeRequest(
  context: HttpRequestProcessingContext,
  timezone: Option[String])

