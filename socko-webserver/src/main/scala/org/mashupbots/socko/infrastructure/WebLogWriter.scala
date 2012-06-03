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

package org.mashupbots.socko.infrastructure

import akka.actor.Actor
import akka.event.Logging

/**
 * Default log writer that writes web log events to the AKKA logger.
 *
 * AKKA logger is used because it asynchronously writes to the log in a separate thread so it does not slow down
 * your app.
 * 
 * @param format Web log format
 */
class WebLogWriter(format: WebLogFormat.Value) extends Actor {
  private val log = Logging(context.system, this)

  /**
   * Process incoming messages
   */
  def receive = {
    case evt: WebLogEvent => {
      format match {
        case WebLogFormat.Common => { log.info(evt.toCommonFormat) }
        case WebLogFormat.Combined => { log.info(evt.toCombinedFormat) }
        case WebLogFormat.Extended => { log.info(evt.toExtendedFormat) }
        case _ => { log.info(evt.toString) }
      }
    }
    case unknown => {
      log.error("WebLogWriter cannot process '{}' messages from '{}'.",
        unknown.toString, sender.path)
    }

  }
}