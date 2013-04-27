//
// Copyright 2013 Vibul Imt* arnasan, David Bolton and Socko contributors.
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

/**
 * Error with your REST definition
 */
case class RestDefintionException(msg: String) extends Exception(msg)

object RestDefintionException {
  def apply(msg: String, cause: Throwable) = new RestDefintionException(msg).initCause(cause)
}

/**
 * Exception raised during if a matching operation cannot be found
 * 
 * @param msg Error message
 */
case class RestNotFoundException(msg: String) extends Exception(msg)

object RestNotFoundException {
  def apply(msg: String, cause: Throwable) = new RestNotFoundException(msg).initCause(cause)
}


/**
 * Exception raised during the process of deserializing and dispatching a request
 * 
 * @param msg Error message
 */
case class RestBindingException(msg: String) extends Exception(msg)

object RestBindingException {
  def apply(msg: String, cause: Throwable) = new RestBindingException(msg).initCause(cause)
}

/**
 * Exception raised during the process of a request
 * 
 * @param msg Error message
 */
case class RestProcessingException(msg: String) extends Exception(msg)

object RestProcessingException {
  def apply(msg: String, cause: Throwable) = new RestProcessingException(msg).initCause(cause)
}
