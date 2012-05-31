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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import java.util.Date
import java.util.GregorianCalendar
import java.net.InetSocketAddress
import org.scalatest.GivenWhenThen

@RunWith(classOf[JUnitRunner])
class DefaultWebLogQueueSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  val evt = new WebLogEvent(
    new Date(),
    clientAddress = new InetSocketAddress("127.0.0.1", 9001),
    serverAddress = new InetSocketAddress("127.0.0.2", 9002),
    username = None,
    method = "GET",
    uri = "/test/file?name1=value1",
    responseStatusCode = 200,
    responseSize = 111,
    requestSize = 222,
    timeTaken = 333,
    protocolVersion = "HTTP/1.1",
    userAgent = None,
    referrer = None)

  "DefaultWebLogQueue" should {

    "enqueue" in {
      val queue = new DefaultWebLogQueue(3)
      queue.enqueue(evt) should be(true)
      queue.size should be(1)

      when("capacity exceeded")
      then("enqueueing shoudl fail")
      queue.enqueue(evt) should be(true)
      queue.enqueue(evt) should be(true)
      queue.enqueue(evt) should be(false)

    }

    "dequeue" in {
      val queue = new DefaultWebLogQueue(3)

      queue.enqueue(evt) should be(true)
      queue.size should be(1)

      val item = queue.dequeue()
      item should equal(evt)
      queue.size should be(0)
    }

    "work in a multi-thread environment" in {
      val queue = new DefaultWebLogQueue(100)

      val p1 = new Thread(new Producer(queue))
      val p2 = new Thread(new Producer(queue))
      val p3 = new Thread(new Producer(queue))
      val c1 = new Thread(new DefaultWebLogWriter(queue, WebLogFormat.Common))

      c1.start()
      p1.start()
      p2.start()
      p3.start()

      while (p1.isAlive && p2.isAlive && p3.isAlive) {
        Thread.sleep(200)
      }

      // Make sure our consumer finishes
      Thread.sleep(1000)
      queue.size should be(0)

      // See if we can shutdown c1 thread
      c1.isAlive should be(true)
      c1.interrupt
      Thread.sleep(100)
      c1.isAlive should be(false)
    }
  }

  class Producer(queue: DefaultWebLogQueue) extends Runnable {
    def run() {
      for (i <- 0 until 50) {
        val evt = new WebLogEvent(
          new Date(),
          clientAddress = new InetSocketAddress("127.0.0.1", 9001),
          serverAddress = new InetSocketAddress("127.0.0.2", 9002),
          username = None,
          method = "GET",
          uri = "/test/file?name1=value1",
          responseStatusCode = 200,
          responseSize = 111,
          requestSize = 222,
          timeTaken = 333,
          protocolVersion = "HTTP/1.1",
          userAgent = None,
          referrer = None)

        queue.enqueue(evt)
      }
    }
  }

}




