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

import java.nio.charset.Charset
import org.jboss.netty.channel.Channel
import java.util.Date

/**
 * Socko Events are fired by Socko and passed to your routes for dispatching to your handlers.
 */
abstract class SockoEvent() {

  /**
   * Netty channel associated with this request
   */
  val channel: Channel

  /**
   * The end point to which the message was addressed
   */
  val endPoint: EndPoint

  /**
   * Store of items that can be used to pass data from route to processor and between processors.
   *
   * This map is not synchronized and not thread-safe. In most cases, we expect this cache to be used by a single
   * thread - hence a standard map is faster.
   *
   * If you do need to use a thread safe map, from your route, instance and store a `ConcurrentHashMap` as an item
   * in this cache.
   */
  lazy val items: collection.mutable.Map[String, Any] = collection.mutable.Map.empty[String, Any]

  /**
   * Timestamp when this event was fired
   */
  val createdOn: Date = new Date()

  /**
   * Number of milliseconds from the time when this context was created
   */
  def duration(): Long = {
    new Date().getTime - createdOn.getTime
  }
  
  /**
   * Username of the authenticated user.  You need to set this for it to appear in the web logs.
   * 
   * Socko does not make assumptions on your authentication method.  You do it and set this `username` to let 
   * us know. 
   */
  var username: Option[String] = None

}

