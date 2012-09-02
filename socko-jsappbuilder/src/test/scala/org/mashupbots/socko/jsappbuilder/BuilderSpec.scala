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
package org.mashupbots.socko.jsappbuilder

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import scala.collection.mutable.ListBuffer
import akka.actor.Props
import org.mashupbots.socko.infrastructure.IOUtil
import java.io.File
import java.util.Date
import java.util.concurrent.TimeoutException

class BuilderSpec extends WordSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterAll {

  "File Copy" in {
    val actorConfig = """
        jsappbuilder {
          rootSourceDirectory = "source"
          rootTargetDirectory = "target"
          tasks = [
            {
              name = "task2"
              source = "/aaa",
              target = "[temp]/bbb"
              tool = "FileCopier"
            }
          ]
		}"""

    val actorSystem = ActorSystem("BuilderSpecClean", ConfigFactory.parseString(actorConfig))
    val cfg = AppConfig(actorSystem)
    
    val listenerMessages = new ListBuffer[BuilderMessage]
    val listener = actorSystem.actorOf(Props(new TestBuilderListener(listenerMessages)), name = "listener")

    val root = IOUtil.createTempDir("BuilderSpec")
    val source = new File(root, "source")
    source.mkdir()

    val target = new File(root, "target")
    target.mkdir()
    IOUtil.writeTextFile(new File(target, "text1.txt"), "stuff1")
    IOUtil.writeTextFile(new File(target, "text2.txt"), "stuff2")
    IOUtil.writeTextFile(new File(target, "text3.txt"), "stuff3")

    val builder = actorSystem.actorOf(Props(new Builder(root, cfg, listener)), name = "builder")
    
    // Clean
    builder ! CleanRequest
    //Waiter.wait(() => { target.list().length == 0 }, 1000, 100, "Waiting for clean request")
    Thread.sleep(1000)
    
    listenerMessages.length should be (4)
    val x = target.listFiles()

    // Finish
    actorSystem.shutdown()
    IOUtil.deleteDir(root)
  }

}

class TestBuilderListener(listenerMessages: ListBuffer[BuilderMessage]) extends Actor {
  def receive = {
    case m: BuilderMessage => listenerMessages.append(m)
  }
}

object Waiter {
  def wait(test: () => Boolean, timeout: Int, sleep: Int, message: String) {
    val start = System.currentTimeMillis()
    while ((System.currentTimeMillis - start < timeout) && !test()) {
      if (sleep > 0) Thread.sleep(sleep)
    }
    throw new TimeoutException(message)
  }
}