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

class JsAppBuilderConfigSpec extends WordSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterAll {

  "Load from Akka Config" in {
    val actorConfig = """
        jsappbuilder {
          rootSourceDirectory = "source"
          rootTargetDirectory = "target"
          rootTempDirectory = "temp"
      
          webserver {
            port=9999
          }
      
          fields = [
            { name = "name1", value = "value1" },
            { name = "name2", value = "value2" }
          ]
      
          tools = [
            {
              name = "tool1"
              actorName = "actor1"
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
              tool = "ClosureCompiler",
	          parameters = [
	            { name = "templating", value = "true" },
	            { name = "compression", value = "false" }
	          ]
            },
            {
              name = "task2"
              source = "/aaa",
              target = "[temp]/bbb"
              tool = "FileCopier"
            }
          ]
		}"""

    val actorSystem = ActorSystem("AppConfigSpecLoad", ConfigFactory.parseString(actorConfig))

    val cfg = AppConfig(actorSystem)

    cfg.rootSourceDirectory should equal("source")
    cfg.rootTargetDirectory should equal("target")
    cfg.rootTempDirectory should equal(Some("temp"))

    cfg.webserver.port should equal(9999)

    cfg.fields.length should equal(2)
    val field1 = cfg.fields(0)
    field1.name should equal("name1")
    field1.value should equal("value1")

    cfg.tools.length should equal(1 + JsAppBuilderConfig.StandardTools.length)
    val tool1 = cfg.tools(0)
    tool1.name should equal("tool1")
    tool1.actorName should equal("actor1")

    cfg.tasks.length should equal(2)
    val t1 = cfg.tasks(0)
    t1.name should equal("task1")
    t1.profile.length should equal(2)
    t1.profile(0) should equal("debug")
    t1.profile(1) should equal("production")
    t1.source should equal("js")
    t1.target should equal("js")
    t1.exclude(0) should equal(".txt")
    t1.exclude(1) should equal("/bin")
    t1.watch should be(false)
    t1.tool should be("ClosureCompiler")
    t1.parameters.length should equal(2)
    t1.parameters(0).name should equal("templating")
    t1.parameters(0).value should equal("true")
    t1.parameters(1).name should equal("compression")
    t1.parameters(1).value should equal("false")

    val t2 = cfg.tasks(1)
    t2.name should equal("task2")
    t2.profile.length should equal(0)
    t2.source should equal("/aaa")
    t2.target should equal("[temp]/bbb")
    t2.exclude.length should equal(0)
    t2.watch should be(true)
    t2.tool should be("FileCopier")
    t2.parameters.length should equal(0)

    actorSystem.shutdown()
  }

  "Empty actions and fields" in {
    val actorConfig = """
        jsappbuilder {
          rootSourceDirectory = "source"
          rootTargetDirectory = "target"
          tasks = [
            {
              name = "task2"
              source = "aaa",
              target = "bbb"
              tool = "FileCopier"
            }
          ]
		}"""

    val cfg = new JsAppBuilderConfig(ConfigFactory.parseString(actorConfig), "jsappbuilder")

    cfg.rootSourceDirectory should equal("source")
    cfg.rootTargetDirectory should equal("target")
    cfg.rootTempDirectory should be(None)

    cfg.webserver.port should equal(8888)

    cfg.fields.length should equal(0)
    cfg.tools.length should equal(JsAppBuilderConfig.StandardTools.length)
    cfg.tasks.length should equal(1)

  }

  "Check for unique field names" in {
    val cfg = JsAppBuilderConfig(
      fields = NameValueConfig("a", "1") :: NameValueConfig("a", "2") :: NameValueConfig("b", "3") :: Nil,
      tools = JsAppBuilderConfig.StandardTools,
      tasks = TaskConfig("task1", Nil, "src", "target", Nil, true, "tool1", Nil) :: Nil)

    val ex = intercept[IllegalArgumentException] {
      cfg.validate()
    }
    
    assert(ex.getMessage.contains("Field name"),
      "'Field names' does not appear in the error message: " + ex.getMessage)
  }
  
  "Check for unique tasks names" in {
    val cfg = JsAppBuilderConfig(
      fields = Nil,
      tools = JsAppBuilderConfig.StandardTools,
      tasks = TaskConfig("task1", Nil, "src", "target", Nil, true, "tool1", Nil) :: 
        TaskConfig("task1", Nil, "src", "target", Nil, true, "tool1", Nil) ::
        TaskConfig("task2", Nil, "src", "target", Nil, true, "tool1", Nil) ::
        TaskConfig("task2", Nil, "src", "target", Nil, true, "tool1", Nil) ::
        TaskConfig("task3", Nil, "src", "target", Nil, true, "tool1", Nil) ::
        Nil)

    val ex = intercept[IllegalArgumentException] {
      cfg.validate()
    }
    assert(ex.getMessage.contains("Task name"),
      "'Task names' does not appear in the error message: " + ex.getMessage)
  }
  
  "Check for unique tool names" in {
    val cfg = JsAppBuilderConfig(
      fields = Nil,
      tools = ToolConfig("FileCopier", "abc") :: JsAppBuilderConfig.StandardTools,
      tasks = TaskConfig("task1", Nil, "src", "target", Nil, true, "tool1", Nil) :: Nil)

    val ex = intercept[IllegalArgumentException] {
      cfg.validate()
    }
    assert(ex.getMessage.contains("Tool name"),
      "'Tool names' does not appear in the error message: " + ex.getMessage)
  }
  
  "Check for empty tasks" in {
    val cfg = JsAppBuilderConfig(
      fields = Nil,
      tools = JsAppBuilderConfig.StandardTools,
      tasks = Nil)

    val ex = intercept[IllegalArgumentException] {
      cfg.validate()
    }
    ex.getMessage should be ("'tasks' property not set")
  }
  
  "Check for valid tool names" in {
    val cfg = JsAppBuilderConfig(
      fields = Nil,
      tools = ToolConfig("MyTool", "abc") :: JsAppBuilderConfig.StandardTools,
      tasks = TaskConfig("task1", Nil, "src", "target", Nil, true, "MyTool", Nil) :: 
        TaskConfig("task2", Nil, "src", "target", Nil, true, "ClosureCompiler", Nil) ::
        TaskConfig("task3", Nil, "src", "target", Nil, true, "xxx", Nil) ::
        Nil)

    val ex = intercept[IllegalArgumentException] {
      cfg.validate()
    }
    assert(ex.getMessage.contains("xxx"),
      "'xxx' does not appear in the error message: " + ex.getMessage)
  }
  
}