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

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpChunkTrailer
import org.jboss.netty.util.CharsetUtil

/**
 * Context for processing HTTP chunks.
 *
 * The `HttpChunkProcessingContext` will only sent to processors:
 *  - if the web server is configured NOT to aggregate chunks
 *  - after an initial `HttpRequestProcessingContext` has been received where the `isChunked` flag is set to `True`
 *
 * @param channel Channel by which the request entered and response will be written
 * @param endPoint End point though which the request entered
 * @param isKeepAlive Flag to indicate if this connection is to be kept alive or closed after a response is returned
 * @param httpChunk Incoming chunk of data for processing
 */
case class HttpChunkProcessingContext(
  channel: Channel,
  originalHttpRequest: OriginalHttpRequest,
  httpChunk: HttpChunk) extends HttpProcessingContext {

  /**
   * HTTP End point
   */
  val endPoint = originalHttpRequest.endPoint

  /**
   * `True` if and only if is connection is to be kept alive and the channel should NOT be closed
   * after a response is returned.
   *
   * This flag is controlled by the existence of the keep alive HTTP header.
   * {{{
   * Connection: keep-alive
   * }}}
   */
  val isKeepAlive = originalHttpRequest.isKeepAlive

  /**
   * Array of accepted encoding for content compression from the HTTP header
   *
   * For example, give then header `Accept-Encoding: gzip, deflate`, then an array containing
   * `gzip` and `defalte` will be returned.
   */
  val acceptedEncodings = originalHttpRequest.acceptedEncodings

  /**
   * Flag to indicate if this is the last chunk
   */
  val isLastChunk = httpChunk.isLast

  /**
   * Headers associated with the last chunk
   */
  def lastChunkHeaders = if (isLastChunk) (httpChunk.asInstanceOf[HttpChunkTrailer]).getHeaders().toList else Nil

  /**
   * Get content as a UTF8 string. Empty string is returned if there is no content.
   */
  def readStringContent(): String = {
    readStringContent(CharsetUtil.UTF_8)
  }

  /**
   * Get content as a string. Empty string is returned if there is no content.
   *
   * @param charset Character set to use to convert data to string
   */
  def readStringContent(charset: Charset): String = {
    var content = httpChunk.getContent
    if (content.readable) content.toString(charset) else ""
  }

  /**
   * Get content as byte array. Empty array is returned if there is no content
   */
  def readBinaryContent(): Array[Byte] = {
    var content = httpChunk.getContent
    if (content.readable) content.array else Array.empty[Byte]
  }

}