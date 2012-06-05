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

import java.util.Date
import org.mashupbots.socko.concurrentlinkedhashmap.ConcurrentLinkedHashMap

/**
 * Local in-memory cache based on Google's [[http://code.google.com/p/concurrentlinkedhashmap ConcurrentLinkedHashMap]].
 *
 * @param capacity Number of items to store. Items in the cache are evicted after this capacity is reached.
 *   Defaults to 1000 items.
 * @param concurrentThreadCount Number of threads that can concurrently modify the cache. Using a significantly
 *   higher or lower value than needed can waste space or lead to thread contention, but an estimate within an order
 *   of magnitude of the ideal value does not usually have a noticeable impact. Because placement in hash tables
 *   is essentially random, the actual concurrency will vary. Defaults to 16.
 */
class LocalCache(capacity: Long = 1000, concurrentThreadCount: Int = 16) {

  private val cache: ConcurrentLinkedHashMap[String, Any] = new ConcurrentLinkedHashMap.Builder[String, Any]()
    .maximumWeightedCapacity(capacity)
    .concurrencyLevel(concurrentThreadCount)
    .build()

  private[LocalCache] case class CachedItem(value: Any, expiry: Long)

  /**
   * Retrieves the value associated with the specified key
   *
   *  @param key Key used in `set`
   *  @return The value associated with the key. `None` if key not present in the cache.
   */
  def get(key: String): Option[Any] = {
    require(key != null, "Key cannot be null")
    val v = cache.get(key)
    if (v == null) {
      None
    } else {
      val i = v.asInstanceOf[CachedItem]
      if (i.expiry > 0 && new Date().getTime > i.expiry) {
        cache.remove(key)
        None
      } else {
        Some(i.value)
      }
    }
  }

  /**
   * Puts a value in the cache
   *
   * @param key Unique identifier for the value
   * @param value Value to cache
   * @param timeToLive After this number of milliseconds, item will be evicted. If `0` (default), item will stay in
   *   the cache indefinitely until the cache's capacity is reached.
   */
  def set(key: String, value: Any, timeToLive: Long = 0) {
    require(key != null, "Key cannot be null")
    require(value != null, "Value cannot be null")
    require(timeToLive >= 0, "timeToLive cannot be less than 0")

    val expiry = if (timeToLive == 0) 0 else new Date().getTime + timeToLive
    cache.put(key, CachedItem(value, expiry))
  }

  /**
   * Removes the value identified by the key from the cache
   *
   * @param key Unique identifier for the value
   */
  def remove(key: String) {
    cache.remove(key)
  }

}

