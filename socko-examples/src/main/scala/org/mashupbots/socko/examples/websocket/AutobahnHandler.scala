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
package org.mashupbots.socko.examples.websocket

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.WebSocketFrameEvent

import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging

/**
 * Echo web socket frames for the Autobahn test suite
 */
class AutobahnHandler extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case event: WebSocketFrameEvent =>
      // Echo framed that was received
      event.context.writeAndFlush(event.wsFrame)
    case _ => {
      log.info("received unknown message of type: ")
    }
  }

}

