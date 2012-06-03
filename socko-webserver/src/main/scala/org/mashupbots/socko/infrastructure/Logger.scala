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

import org.slf4j.LoggerFactory

/**
 * Adds logging functionality to our classes.
 * 
 * We try not to use this logger too often because it is synchronous. 
 * Most of Socko's logging is performed inside Akka because it is asynchronous.
 * 
 * Usage:
 * {{{
 * log.debug("Hello")
 * log.error("Message: {} {}", Array[Object]("value1", "value2"))
 * }}}
 */
trait Logger {
  /**
   * Logger name is inferred from the class name.
   */
  lazy val log = LoggerFactory.getLogger(this.getClass)
}