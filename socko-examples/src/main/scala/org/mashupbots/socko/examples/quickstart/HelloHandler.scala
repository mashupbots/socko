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
package org.mashupbots.socko.examples.quickstart

import org.mashupbots.socko.events.HttpRequestEvent
import akka.actor.Actor
import java.util.Date

/**
 * Hello processor writes a greeting and stops.
 */
class HelloHandler extends Actor {
  def receive = {
    case event: HttpRequestEvent =>
      event.response.write("Hello from Socko (" + new Date().toString + ")")
      context.stop(self)
  }
}

