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
package org.mashupbots.socko.events

/**
 * Abstract event triggered on HTTP related activity
 */
trait HttpEvent extends SockoEvent {

  /**
   * Event processing configuration
   */
  val config: HttpEventConfig

  /**
   * Write a web log entry
   *
   * @param responseStatusCode HTTP status code
   * @param responseSize length of response content in bytes
   */
  def writeWebLog(responseStatusCode: Int, responseSize: Long)
}
