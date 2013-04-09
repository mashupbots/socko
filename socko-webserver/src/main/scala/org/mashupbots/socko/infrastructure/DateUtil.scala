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

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

/**
 * Utility methods associated with dates
 */
object DateUtil {
  /**
   * RFC 1123 Date Format
   */
  val RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"

  /**
   * RFC 1123 Date timezone
   */
  val RFC1123_DATE_GMT_TIMEZONE = "GMT"

  /**
   * Returns a date formatted for RFC 1123 format like `Wed, 02 Oct 2002 13:00:00 GMT`.
   *
   * RFC 1123 is an updated version of RFC 822.
   *
   * See [[http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3 HTTP 1.1 specs]]
   */
  def rfc1123DateFormatter() = {
    val df = new SimpleDateFormat(RFC1123_DATE_FORMAT, Locale.US)
    df.setTimeZone(TimeZone.getTimeZone(RFC1123_DATE_GMT_TIMEZONE))
    df
  }

  /**
   * Parse different forms of ISO8601 date formats.  The following variants are supported
   *  - 2001-07-04
   *  - 2001-07-04T12:08:56
   *  - 2001-07-04T12:08:56Z
   *  - 2001-07-04T12:08:56.235
   *  - 2001-07-04T12:08:56-0700
   *  - 2001-07-04T12:08:56.235Z
   *  - 2001-07-04T12:08:56.235-0700
   *
   * @param s String to parse
   * @return Date in local time
   */
  def parseISO8601Date(s: String): Date = {
    if (s == null || s.isEmpty()) {
      throw new IllegalArgumentException("Null or empty string")
    } else {
      s.length match {
        case 10 => new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)
        case 19 => new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(s)
        case 20 => {
          val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
          df.setTimeZone(TimeZone.getTimeZone("UTC"))
          df.parse(s)
        }
        case 23 => new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).parse(s)
        case 24 => {
          if (s.endsWith("Z")) {
            val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            df.setTimeZone(TimeZone.getTimeZone("UTC"))
            df.parse(s)
          } else {
        	new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(s)
          }
        }
        case 28 => new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).parse(s)
        case _ => throw new IllegalArgumentException("Cannot parse date: " + s)
      }
    }
  }

  def formatISO8601Date(d: Date): String = {
    new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)
  }

  def formatISO8601DateTime(d: Date): String = {
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(d)
  }

  def formatISO8601UTCDateTime(d: Date): String = {
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
    df.format(d)
  }

}