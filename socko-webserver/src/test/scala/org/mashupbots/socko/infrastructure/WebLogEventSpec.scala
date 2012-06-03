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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import java.util.Date
import java.util.GregorianCalendar
import java.net.InetSocketAddress

@RunWith(classOf[JUnitRunner])
class WebLogEventSpec extends WordSpec with ShouldMatchers {

  "WebLogEvent" should {

    "be able to output Common web log format with no username" in {
      val ts = new GregorianCalendar(2012, 4, 14, 1, 2, 3)
      ts.setTimeZone(WebLogEvent.UTC_TZ)

      val evt = WebLogEvent(
        timestamp = ts.getTime,
        "server1",
        100,
        clientAddress = new InetSocketAddress("127.0.0.1", 9001),
        serverAddress = new InetSocketAddress("127.0.0.2", 9002),
        username = None,
        method = "GET",
        uri = "/test/file?name1=value1",
        requestSize = 222,
        responseStatusCode = 200,
        responseSize = 111,
        timeTaken = 333,
        protocolVersion = "HTTP/1.1",
        userAgent = Some("Mozilla/4.0+(compatible;+MSIE+4.01;+Windows+95)"),
        referrer = Some("http://www.somesite.net"))

      evt.toCommonFormat should be(
        "127.0.0.1 - - [14/May/2012:01:02:03 +0000] \"GET /test/file?name1=value1 HTTP/1.1\" 200 111")
    }

    "be able to output Common web log format with username" in {
      val ts = new GregorianCalendar(2012, 4, 14, 1, 2, 3)
      ts.setTimeZone(WebLogEvent.UTC_TZ)

      val evt = WebLogEvent(
        timestamp = ts.getTime,
        "server1",
        100,
        clientAddress = new InetSocketAddress("127.0.0.1", 9001),
        serverAddress = new InetSocketAddress("127.0.0.2", 9002),
        username = Some("my name"), // added space to make sure we strip it
        method = "GET",
        uri = "/test/file?name1=value1",
        requestSize = 222,
        responseStatusCode = 200,
        responseSize = 111,
        timeTaken = 333,
        protocolVersion = "HTTP/1.0",
        userAgent = None,
        referrer = None)

      evt.toCommonFormat should be(
        "127.0.0.1 - my+name [14/May/2012:01:02:03 +0000] \"GET /test/file?name1=value1 HTTP/1.0\" 200 111")
    }

   "be able to output Combined web log format without querystring, username, useragent and referrer" in {
      val ts = new GregorianCalendar(2012, 4, 14, 1, 2, 3)
      ts.setTimeZone(WebLogEvent.UTC_TZ)

      val evt = WebLogEvent(
        timestamp = ts.getTime,
        "server1",
        100,
        clientAddress = new InetSocketAddress("127.0.0.1", 9001),
        serverAddress = new InetSocketAddress("127.0.0.2", 9002),
        username = None,
        method = "GET",
        uri = "/test/file.txt",
        requestSize = 222,
        responseStatusCode = 200,
        responseSize = 111,
        timeTaken = 333,
        protocolVersion = "HTTP/1.1",
        userAgent = None,
        referrer = None)

      evt.toCombinedFormat should be(
        "127.0.0.1 - - [14/May/2012:01:02:03 +0000] \"GET /test/file.txt HTTP/1.1\" 200 111 - -")
    }

    "be able to output Combined web log format with querystring, username, useragent and referrer" in {
      val ts = new GregorianCalendar(2012, 4, 14, 1, 2, 3)
      ts.setTimeZone(WebLogEvent.UTC_TZ)

      val evt = WebLogEvent(
        timestamp = ts.getTime,
        "server1",
        100,
        clientAddress = new InetSocketAddress("127.0.0.1", 9001),
        serverAddress = new InetSocketAddress("127.0.0.2", 9002),
        username = Some("my name"), // added space to make sure we strip it
        method = "GET",
        uri = "/test/file.txt?name1=value1",
        requestSize = 222,
        responseStatusCode = 200,
        responseSize = 111,
        timeTaken = 333,
        protocolVersion = "HTTP/1.0",
        userAgent = Some("Mozilla/4.0 (compatible; MSIE 4.01; Windows 95)"),
        referrer = Some("http://www.somesite.net"))

      evt.toCombinedFormat should be(
        "127.0.0.1 - my+name [14/May/2012:01:02:03 +0000] \"GET /test/file.txt?name1=value1 HTTP/1.0\" 200 111 \"http://www.somesite.net\" \"Mozilla/4.0 (compatible; MSIE 4.01; Windows 95)\"")
    }    
    
    "be able to output Extended web log format without querystring, username, useragent and referrer" in {
      val ts = new GregorianCalendar(2012, 4, 14, 1, 2, 3)
      ts.setTimeZone(WebLogEvent.UTC_TZ)

      val evt = WebLogEvent(
        timestamp = ts.getTime,
        "server1",
        100,
        clientAddress = new InetSocketAddress("127.0.0.1", 9001),
        serverAddress = new InetSocketAddress("127.0.0.2", 9002),
        username = None,
        method = "GET",
        uri = "/test/file.txt",
        requestSize = 222,
        responseStatusCode = 200,
        responseSize = 111,
        timeTaken = 333,
        protocolVersion = "HTTP/1.1",
        userAgent = None,
        referrer = None)

      evt.toExtendedFormat should be(
        "2012-05-14 01:02:03 127.0.0.1 - 127.0.0.2 9002 GET /test/file.txt - 200 111 222 333 - -")
    }

    "be able to output Extended web log format with querystring, username, useragent and referrer" in {
      val ts = new GregorianCalendar(2012, 4, 14, 1, 2, 3)
      ts.setTimeZone(WebLogEvent.UTC_TZ)

      val evt = WebLogEvent(
        timestamp = ts.getTime,
        "server1",
        100,
        clientAddress = new InetSocketAddress("127.0.0.1", 9001),
        serverAddress = new InetSocketAddress("127.0.0.2", 9002),
        username = Some("my name"), // added space to make sure we strip it
        method = "GET",
        uri = "/test/file.txt?name1=value1",
        requestSize = 222,
        responseStatusCode = 200,
        responseSize = 111,
        timeTaken = 333,
        protocolVersion = "HTTP/1.0",
        userAgent = Some("Mozilla/4.0 (compatible; MSIE 4.01; Windows 95)"),
        referrer = Some("http://www.somesite.net"))

      evt.toExtendedFormat should be(
        "2012-05-14 01:02:03 127.0.0.1 my+name 127.0.0.2 9002 GET /test/file.txt name1=value1 200 111 222 333 Mozilla/4.0+(compatible;+MSIE+4.01;+Windows+95) http://www.somesite.net")
    }

  }
}