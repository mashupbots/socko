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

import java.nio.charset.Charset

/**
 * A utility class that provides various constants related with character sets.
 */
object CharsetUtil {

  /**
   * 16-bit UTF (UCS Transformation Format) whose byte order is identified by
   * an optional byte-order mark
   */
  val UTF_16 = Charset.forName("UTF-16")

  /**
   * 16-bit UTF (UCS Transformation Format) whose byte order is big-endian
   */
  val UTF_16BE = Charset.forName("UTF-16BE")

  /**
   * 16-bit UTF (UCS Transformation Format) whose byte order is little-endian
   */
  val UTF_16LE = Charset.forName("UTF-16LE")

  /**
   * 8-bit UTF (UCS Transformation Format)
   */
  val UTF_8 = Charset.forName("UTF-8")

  /**
   * ISO Latin Alphabet No. 1, as known as <tt>ISO-LATIN-1</tt>
   */
  val ISO_8859_1 = Charset.forName("ISO-8859-1")

  /**
   * 7-bit ASCII, as known as ISO646-US or the Basic Latin block of the
   * Unicode character set
   */
  val US_ASCII = Charset.forName("US-ASCII")

}