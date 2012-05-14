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

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Queues web log events so that they can be asynchronously written to the logger for better performance.
 *
 * We have implemented using `LinkedBlockingQueue` because it seems to be the better performer. See
 * [[http://www.javacodegeeks.com/2010/09/java-best-practices-queue-battle-and.html here]] and 
 * [[http://codeidol.com/java/javagenerics/Queues/Comparing-Queue-Implementations/ here]].
 *
 * Credits also goes to this Twitter [[http://twitter.github.com/scala_school/concurrency.html tutorial]].
 *
 * @param capacity Maximum number of events this queue will hold.  Excess elements are discarded.
 */
class DefaultWebLogQueue(val capacity: Int) extends WebLogQueue {

  private val queue = new LinkedBlockingQueue[WebLogEvent](capacity)

  /**
   * Inserts the specified element at the tail of this queue if it is possible to do so immediately without
   * exceeding the queue's capacity
   *
   * @param evt web log event to queue
   * @returns `true` upon success and `false` if this queue is full.
   */
  def enqueue(evt: WebLogEvent): Boolean = {
    queue.offer(evt)
  }

  /**
   * Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
   *
   * @returns A queued `WebLogEvent`
   */
  def dequeue(): WebLogEvent = {
    queue.take
  }

  /**
   * Retrieves and removes the head of this queue, waiting for the specified time.
   *
   * @param timeoutMilliSeconds Number of milliseconds to wait before returnin
   * @returns A queued `WebLogEvent`, `None` if timed out
   */
  def dequeue(timeoutMilliSeconds: Long): Option[WebLogEvent] = {
    val v = queue.poll(timeoutMilliSeconds, TimeUnit.MILLISECONDS)
    if (v == null) None else Some(v)
  }

  /**
   * Returns the number of web log events in this queue.
   */
  def size(): Int = {
    queue.size
  }

}

