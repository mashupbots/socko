//
// Copyright 2013 Vibul Imtarnasan, David Bolton and Socko contributors.
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

import scala.collection.mutable.ArrayBuffer

/**
 * Represents a HTTP header field
 */
case class HttpHeader(name: String, value: String)

/**
 * Immutable and read only collection of HTTP headers
 *
 * Unlike a HashMap, this collection supports multiple entries with the same field name and name matching
 * is performed in a case insensitive manner as per [[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]].
 */
case class ImmutableHttpHeaders(private val values: Seq[HttpHeader]) extends Iterable[HttpHeader] {

  /**
   * Creates a new iterator over all headers
   */
  def iterator: Iterator[HttpHeader] = values.iterator

  /**
   * Returns the value of the first header that has a matching name.
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @return None if not found, else the value of the first header with a matching name
   */
  def get(name: String): Option[String] = {
    val result = values.find(h => h.name.equalsIgnoreCase(name))
    if (result.isEmpty) None
    else Some(result.get.value)
  }

  /**
   * Returns the value of the first header that matches the name.
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @param defaultValue value to return if header not found
   * @return Default value if not found, else the value of the first header with a matching name
   */
  def getOrElse(name: String, defaultValue: String): String = {
    val result = values.find(h => h.name.equalsIgnoreCase(name))
    if (result.isEmpty) defaultValue
    else result.get.value
  }

  /**
   * Returns the values of all matching headers
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @return Sequence of matching values
   */
  def getAll(name: String): Seq[String] = {
    values.filter(h => h.name.equalsIgnoreCase(name)).map(h => h.value)
  }

  /**
   * Returns true if there is a header with the specified name
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @return True if there is a header with the specified name, false otherwise
   */
  def contains(name: String): Boolean = {
    values.exists(h => h.name.equalsIgnoreCase(name))
  }
}

/**
 * Companion object
 */
object ImmutableHttpHeaders {

  /**
   * Alternative constructor
   */
  def apply(values: Iterable[(String, String)]) = new ImmutableHttpHeaders(values.map(t => HttpHeader(t._1, t._2)).toSeq)

  /**
   * No headers
   */
  val empty = new ImmutableHttpHeaders(Nil)

}

/**
 * Mutable collection of HTTP headers
 *
 * Unlike a HashMap, this collection supports multiple entries with the same field name and name matching
 * is performed in a case insensitive manner as per [[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]].
 */
case class MutableHttpHeaders() extends Iterable[HttpHeader] {

  private val values: ArrayBuffer[HttpHeader] = new ArrayBuffer()

  /**
   * Creates a new iterator over all headers
   */
  def iterator: Iterator[HttpHeader] = values.iterator

  /**
   * Returns the value of the first header that has a matching name.
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @return None if not found, else the value of the first header with a matching name
   */
  def get(name: String): Option[String] = {
    val result = values.find(h => h.name.equalsIgnoreCase(name))
    if (result.isEmpty) None
    else Some(result.get.value)
  }

  /**
   * Returns the value of the first header that matches the name.
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @param defaultValue value to return if header not found
   * @return Default value if not found, else the value of the first header with a matching name
   */
  def getOrElse(name: String, defaultValue: String): String = {
    val result = values.find(h => h.name.equalsIgnoreCase(name))
    if (result.isEmpty) defaultValue
    else result.get.value
  }

  /**
   * Returns the values of all matching headers
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @return Sequence of matching values
   */
  def getAll(name: String): Seq[String] = {
    values.filter(h => h.name.equalsIgnoreCase(name)).toSeq.map(h => h.value)
  }

  /**
   * Returns true if there is a header with the specified name
   *
   * @param name Name of header field to match in a case insensitive manner ([[http://www.ietf.org/rfc/rfc2616.txt RFC 2616]])
   * @return True if there is a header with the specified name, false otherwise
   */
  def contains(name: String): Boolean = {
    values.exists(h => h.name.equalsIgnoreCase(name))
  }

  /**
   * Adds the specified header to the collection. If an existing entry with the same name exists,
   * the old entries are replaced with the new value.
   *
   * Matching of names is performed in a case insensitive manner
   *
   * @param name Name of header field.
   * @param value Value of the header
   */
  def put(name: String, value: String) = {
    remove(name)
    append(name, value)
  }

  /**
   * Adds the specified header to the collection. If an existing entry with the same name exists,
   * the old entries are replaced with the new value.
   *
   * @param header HTTP header that will be put into the collection
   */
  def put(header: HttpHeader) = {
    remove(header.name)
    append(header)
  }

  /**
   * Appends the specified header to the end of the collection.
   *
   * No checks are performed for uniqueness.  This methods can be use for example, to append multiple cookies
   * header fields.
   *
   * @param name Name of header field.
   * @param value Value of the header
   */
  def append(name: String, value: String) = {
    values.append(HttpHeader(name, value))
  }

  /**
   * Appends the specified header to the end of the collection.
   *
   * No checks are performed for uniqueness.  This methods can be use for example, to append multiple cookies
   * header fields.
   *
   * @param header HTTP header that will be appended
   */
  def append(header: HttpHeader) = {
    values.append(header)
  }

  /**
   * Removes all headers of with the specified name
   *
   * @param name Name of header field to remove
   * @return The number of headers removed
   */
  def remove(name: String): Int = {
    def removeHelper(name: String, startIdx: Int, count: Int): Int = {
      val idx = values.indexWhere(h => h.name.equalsIgnoreCase(name), startIdx)
      if (idx == -1) count
      else {
        values.remove(idx)
        removeHelper(name, idx, count + 1)
      }
    }
    removeHelper(name, 0, 0)
  }

}

/**
 * Companion object
 */
object MutableHttpHeaders {

  /**
   * Alternative constructor
   * 
   * @param values Collection of string tuple
   */
  def apply(values: Iterable[(String, String)]) = {
    val v = new MutableHttpHeaders()
    values.foreach(f => v.append(f._1, f._2))
    v
  }

  /**
   * Alternative constructor
   * 
   * @param headers Collection of HTTP Headers
   */
  def apply(headers: ImmutableHttpHeaders) = {
    val v = new MutableHttpHeaders()
    headers.foreach(h => v.append(h))
    v
  }
  
  /**
   * Alternative copy constructor
   * 
   * @param headers Collection of HTTP Headers
   */
  def apply(headers: MutableHttpHeaders) = {
    val v = new MutableHttpHeaders()
    headers.foreach(h => v.append(h))
    v
  }  
}
