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

import akka.util.Duration

/**
 * Cache used by Socko to store your objects
 */
trait Cache {

  /**
   * Retrieves the value associated with the specified key
   *
   *  @param key Key used in `set`
   *  @return The value associated with the key. `None` if key not present in the cache.
   */
  def get(key: String): Option[Any]

  /**
   * Stores a value in the cache
   *
   * @param key Unique identifier for the value
   * @param value Value to cache
   * @param timeToLive After this number of milliseconds, item will be evicted. If `0` (default), item will stay in 
   *   the cache indefinitely until the cache's capacity is reached. 
   */
  def set(key: String, value: Any, timeToLive: Long = 0)

  /**
   * Removes the value identified by the key from the cache
   *
   * @param key Unique identifier for the value
   */
  def remove(key: String)
}
