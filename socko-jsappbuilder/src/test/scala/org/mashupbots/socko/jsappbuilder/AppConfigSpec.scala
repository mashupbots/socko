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

  "Load from Akka Config" in {
    val actorConfig = """
        jsappbuilder {
          source = "source"
          target = "target"
      
          webserver {
            port=9999
          }
      
          fields = [
            { name = "name1", value = "value1" },
            { name = "name2", value = "value2" }
          ]
      
          actions = [
            {
              name = "customAction"
              actorName = "customActorName"
	          defaults = [
	            { name = "name1", value = "value1" },
	            { name = "name2", value = "value2" }
	          ]
            }
          ]
      
          tasks = [
            {
              name = "task1",
    		  profile = ["debug", "production"],
              source = "js",
              target = "js",
              exclude = [".txt", "/bin"],
              watch = false,
              action = "javascript",
	          parameters = [
	            { name = "templating", value = "true" },
	            { name = "compression", value = "false" }
	          ]
            },
            {
              name = "task2"
              source = "aaa",
              target = "bbb"
              action = "copy"
            }
          ]
		}"""

    val actorSystem = ActorSystem("AppConfigSpecLoad", ConfigFactory.parseString(actorConfig))

    val cfg = AppConfig(actorSystem)

    cfg.source should equal("source")
    cfg.target should equal("target")

    cfg.webserver.port should equal(9999)

    cfg.fields.length should equal(2)    
    val field1 = cfg.fields(0)
    field1.name should equal("name1")
    field1.value should equal ("value1")

    cfg.actions.length should equal(1)    
    val action1 = cfg.actions(0)
    action1.name should equal("customAction")
    action1.actorName should equal("customActorName")
    action1.defaults.length should equal(2)
    action1.defaults(0).name should equal("name1")
    action1.defaults(0).value should equal("value1")
    
    cfg.tasks.length should equal (2)    
    val t1 = cfg.tasks(0)
    t1.name should equal("task1")
    t1.profile.length should equal (2)
    t1.profile(0) should equal ("debug")
    t1.profile(1) should equal ("production")
    t1.source should equal("js")
    t1.target should equal("js")
    t1.exclude(0) should equal (".txt")
    t1.exclude(1) should equal ("/bin")
    t1.watch should be (false)
    t1.action should be ("javascript")
    t1.parameters.length should equal(2)
    t1.parameters(0).name should equal("templating")
    t1.parameters(0).value should equal("true")
    t1.parameters(1).name should equal("compression")
    t1.parameters(1).value should equal("false")

    val t2 = cfg.tasks(1)
    t2.name should equal("task2")
    t2.profile.length should equal (0)
    t2.source should equal("aaa")
    t2.target should equal("bbb")
    t2.exclude.length should equal (0)
    t2.watch should be (true)
    t2.action should be ("copy")
    t2.parameters.length should equal(0)
    
    actorSystem.shutdown()
  }
  
  "Empty actions and fields" in {
    val actorConfig = """
        jsappbuilder {
          source = "source"
          target = "target"
          tasks = [
            {
              name = "task2"
              source = "aaa",
              target = "bbb"
              action = "copy"
            }
          ]
		}"""

    val actorSystem = ActorSystem("AppConfigSpecEmpty", ConfigFactory.parseString(actorConfig))

    val cfg = AppConfig(actorSystem)

    cfg.source should equal("source")
    cfg.target should equal("target")

    cfg.webserver.port should equal(8888)

    cfg.fields.length should equal(0)    
    cfg.actions.length should equal(0)    
    cfg.tasks.length should equal (1)    
    
    actorSystem.shutdown()
  }  

}