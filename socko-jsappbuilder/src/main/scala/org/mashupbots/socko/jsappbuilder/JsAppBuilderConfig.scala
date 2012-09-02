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

import java.io.File
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ListBuffer
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.webserver.WebServerConfig
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import org.mashupbots.socko.infrastructure.ConfigUtil

/**
 * Configuration for JsAppBuilder
 *
 * @param rootSourceDirectory Path to source folder. Maybe full path or relative to the directory of the configuration
 *   file
 * @param rootTargetDirectory Path to target folder when output files will be stored. Maybe full path or relative to
 *   the directory of the configuration file
 * @param rootTempDirectory Path to directory to store temporary files. If `None`, then one will be created using
 *   JDK settings.
 * @param webserver Configuration of webserver for serving files from the target directory
 * @param fields List of fields for placeholder substitutions
 * @param tools List of custom tools created by the user
 * @param tasks List of build tasks
 */
case class JsAppBuilderConfig(
  rootSourceDirectory: String = "src",
  rootTargetDirectory: String = "target",
  rootTempDirectory: Option[String] = None,
  webserver: WebServerConfig = WebServerConfig(),
  fields: List[NameValueConfig] = Nil,
  tools: List[ToolConfig] = JsAppBuilderConfig.StandardTools,
  tasks: List[TaskConfig] = Nil) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    ConfigUtil.getString(config, prefix + ".rootSourceDirectory", "src"),
    ConfigUtil.getString(config, prefix + ".rootTargetDirectory", "target"),
    ConfigUtil.getOptionalString(config, prefix + ".rootTempDirectory"),
    JsAppBuilderConfig.getWebServerConfig(config, prefix + ".webserver"),
    JsAppBuilderConfig.getNameValueConfig(config, prefix + ".fields"),
    JsAppBuilderConfig.getToolsConfig(config, prefix + ".tools") ::: JsAppBuilderConfig.StandardTools,
    JsAppBuilderConfig.getTasksConfig(config, prefix + ".tasks"))

  /**
   * Validate the configuration
   */
  def validate() {
    if (rootSourceDirectory.isEmpty) {
      throw new IllegalArgumentException("'rootSourceDirectory' property not set")
    }
    if (rootTargetDirectory.isEmpty) {
      throw new IllegalArgumentException("'rootTargetDirectory' property not set")
    } 
    if (tasks.isEmpty) {
      throw new IllegalArgumentException("'tasks' property not set")
    } 
    
    webserver.validate()
    tools.foreach(t => t.validate())
    tasks.foreach(t => t.validate())

    // GroupBy transforms our list into Map[ToolName, List[ToolConfig]]
    // Filter removes entries where number of entries is the List[ToolConfig] is less than 2
    // Map returns a List[ToolName]
    val duplicateTools = tools.groupBy(a => a.name).filter(mapEntry => mapEntry._2.length > 1).map(_._1)
    if (duplicateTools.size > 0) {
      throw new IllegalArgumentException("Tool name(s) '%s' not unique".format(duplicateTools.mkString(",")))
    }

    // Check for unique task names
    val duplicateTasks = tasks.groupBy(t => t.name).filter(mapEntry => mapEntry._2.length > 1).map(_._1)
    if (duplicateTasks.size > 0) {
      throw new IllegalArgumentException("Task name(s) '%s' not unique".format(duplicateTasks.mkString(",")))
    }
    
    // Check for unique field names
    val duplicateFields = fields.groupBy(f => f.name).filter(mapEntry => mapEntry._2.length > 1).map(_._1)
    if (duplicateFields.size > 0) {
      throw new IllegalArgumentException("Field name(s) '%s' not unique".format(duplicateFields.mkString(",")))
    }

    // Check task tool names are valid
    val toolNames = tools.map(_.name)
    tasks.foreach(t => {
      if (!toolNames.exists(name => name == t.tool)) {
        throw new IllegalArgumentException("Unrecognised tool '%s' in task '%s'".format(t.tool, t.name))
      }
    })

  }
}

/**
 * Statics for JsAppBuilderConfig
 */
object JsAppBuilderConfig extends Logger {

  /**
   * Returns the defined `WebServerConfig`. If not defined, then the default `WebServerConfig` is returned.
   */
  def getWebServerConfig(config: Config, name: String): WebServerConfig = {
    try {
      val v = config.getConfig(name)
      if (v == null) {
        WebServerConfig()
      } else {
        new WebServerConfig(config, name)
      }
    } catch {
      case ex: ConfigException.Missing => {
        WebServerConfig()
      }
      case ex => {
        log.info("Error parsing WebServerConfig. Defaults will be used.")
        log.debug("Exception", ex)
        WebServerConfig()
      }
    }
  }

  /**
   * Returns the list of `NameValueConfig`. If not defined, an empty list is returned
   */
  def getNameValueConfig(config: Config, name: String): List[NameValueConfig] = {
    try {
      val l = config.getConfigList(name)
      val lb = new ListBuffer[NameValueConfig]
      l.foreach(cfg => lb.append(new NameValueConfig(cfg)))
      lb.toList
    } catch {
      case ex => {
        Nil
      }
    }
  }

  /**
   * List of standard tools
   */
  val StandardTools: List[ToolConfig] = ToolConfig("ClosureCompiler", "abc") ::
    ToolConfig("FileCopier", "abc") ::
    Nil

  /**
   * Returns the list of `ToolConfig`. If not defined, an error is returned
   */
  def getToolsConfig(config: Config, name: String): List[ToolConfig] = {
    try {
      val l = config.getConfigList(name)
      val lb = new ListBuffer[ToolConfig]
      l.foreach(cfg => lb.append(new ToolConfig(cfg)))
      lb.toList
    } catch {
      case ex => {
        Nil
      }
    }
  }

  /**
   * Returns the list of `TaskConfig`. If not defined, an error is returned
   */
  def getTasksConfig(config: Config, name: String): List[TaskConfig] = {
    val l = config.getConfigList(name)
    val lb = new ListBuffer[TaskConfig]

    // Each config in the list returned from getConfigList() contains the definition of a task.
    // l.toString: [Config(SimpleConfigObject({"id" : "1"})), Config(SimpleConfigObject({"id" : "2"}))]
    l.foreach(cfg => lb.append(new TaskConfig(cfg)))

    lb.toList
  }

}

/**
 * Name Value fields used in different places
 *
 * @param name Name of data field
 * @param value Value of data field
 */
case class NameValueConfig(
  name: String,
  value: String) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config) = this(
    config.getString("name"),
    ConfigUtil.getString(config, "value", ""))
}

/**
 * Configuration for a tool
 *
 * @param name Unique identifier for the tool
 * @param actorName Name of AKKA actor in to call to invoke the tool
 */
case class ToolConfig(
  name: String,
  actorName: String) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config) = this(
    ConfigUtil.getString(config, "name", ""),
    ConfigUtil.getString(config, "actorName", ""))

  /**
   * Validate the configuration
   */
  def validate() {
    if (name.isEmpty) {
      throw new IllegalArgumentException("Tool 'name' property not set")
    }
    if (actorName.isEmpty) {
      throw new IllegalArgumentException("Tool 'actorName' property not set for '%s'".format(name))
    }
  }
}

/**
 * Configuration for a build task
 *
 * @param name Unique identifier for the task
 * @param profile List of matching profiles for which this task should be run. Empty list means run this task
 *   for all profiles.
 * @param source Source directory relative to the app config source directory
 * @param target Target directory relative to the app config target directory
 * @param exclude List of patterns to match files in the source directory to exclude from process.  For example,
 *   `.txt` will exclude all files containing `.txt` like `readme.txt` and `some.txt.file.dat`
 * @param watch Flag to indicate if we need to watch the source file for changes. Applicable in `server` mode.
 * @param tool Name of tool to use to execute this task. For example `ClosureCompiler`
 * @param parameters Parameters for the tool.
 */
case class TaskConfig(
  name: String,
  profile: List[String] = Nil,
  source: String,
  target: String,
  exclude: List[String] = Nil,
  watch: Boolean = true,
  tool: String,
  parameters: List[NameValueConfig] = Nil) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config) = this(
    ConfigUtil.getString(config, "name", ""),
    ConfigUtil.getListString(config, "profile"),
    ConfigUtil.getString(config, "source", ""),
    ConfigUtil.getString(config, "target", ""),
    ConfigUtil.getListString(config, "exclude"),
    ConfigUtil.getBoolean(config, "watch", true),
    ConfigUtil.getString(config, "tool", ""),
    JsAppBuilderConfig.getNameValueConfig(config, "parameters"))

  /**
   * Validate the configuration
   */
  def validate() {
    if (name.isEmpty) {
      throw new IllegalArgumentException("Task 'name' property not set")
    }
    if (source.isEmpty) {
      throw new IllegalArgumentException("'source' property of task '%s' not set".format(name))
    }
    if (target.isEmpty) {
      throw new IllegalArgumentException("'target' property of task '%s' not set".format(name))
    }
    if (tool.isEmpty) {
      throw new IllegalArgumentException("'tool' property of task '%s' not set".format(name))
    }
  }
}


