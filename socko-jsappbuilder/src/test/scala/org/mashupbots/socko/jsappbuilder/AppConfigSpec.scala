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

class AppConfigSpec extends WordSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterAll {

  "load from Akka Config" in {

    val actorConfig = """
        jsappbuilder {
          src = "src"
          target = "target"
          webserver {
            port=9999
          }
          tasks = [
            {
              id = "1"
            } 
            {
              id = "2"
            }
          ]
		}"""

    val actorSystem = ActorSystem("AppConfigSpec", ConfigFactory.parseString(actorConfig))

    val cfg = AppConfig(actorSystem)

    cfg.src.getPath should equal("src")
    cfg.target.getPath should equal("target")

    cfg.webserver.port should equal(9999)

    cfg.tasks.length should equal (2)
    
    val t1 = cfg.tasks(0)
    t1.id should equal("1")

    val t2 = cfg.tasks(1)
    t2.id should equal("2")
    
    actorSystem.shutdown()
  }

}