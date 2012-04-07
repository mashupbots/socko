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
package org.mashupbots.socko.context

import java.nio.charset.Charset

import org.jboss.netty.channel.Channel

/**
 * When processing a web request, the context provides a uniform way to read request data
 * and write response data.
 * 
 * ProcessingContext are created by Socko handlers and passed to Socko processors via routes.
 */
abstract class ProcessingContext() {

  /**
   * Netty channel associated with this request
   */
  def channel: Channel

  /**
   * The end point to which the request was addressed
   */
  def endPoint: EndPoint

  /**
   * Cache that can be use to pass data from handler to processor and between processors
   */
  val cache: collection.mutable.Map[String, String] = collection.mutable.Map.empty[String, String]

  /**
   * Returns the request content as a string
   */
  def readStringContent(): String

  /**
   * Returns the request content as a string
   *
   * @param charset Character set to use to decode binary data into a string
   */
  def readStringContent(charset: Charset): String

  /**
   * Returns the request content as a byte array
   */
  def readBinaryContent(): Array[Byte]
}

