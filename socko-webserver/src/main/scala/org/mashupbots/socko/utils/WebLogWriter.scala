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
package org.mashupbots.socko.utils

/**
 * Reads activity log queues and writes them to the logger.
 *
 * @param queue Web Log Queue to monitor and dequeue
 * @param format Format of log to write
 */
class WebLogWriter(queue: WebLogQueue, format: WebLogFormat.Type) extends Runnable with Logger {

  var isRunning = false

  def run() {
    try {
      isRunning = true
      while (isRunning) {
        val evt = queue.dequeue

        try {
          if (format == WebLogFormat.Common) {
            log.info(evt.toCommonFormat)
          } else if (format == WebLogFormat.Common) {
            log.info(evt.toExtendedFormat)
          }
        } catch {
          case _ => //Ignore any errors and write next event
        }

      }
    } catch {
      case _ => //Don't re-throw any errors and quietly die
    }

  }

  def stop() {
    isRunning = false
  }

}