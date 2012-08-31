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
 * Configuration settings for our app
 */
object AppConfig extends ExtensionId[AppConfigImpl] with ExtensionIdProvider {
  override def lookup = AppConfig
  override def createExtension(system: ExtendedActorSystem) =
    new AppConfigImpl(system.settings.config, "jsappbuilder")
}

/**
 * Implementation class for our app configuration
 *
 * @param source Path to source folder. Maybe full path or relative to the directory of the configuration file
 * @param target Path to target folder when output files will be stored. Maybe full path or relative to the directory
 *   of the configuration file
 * @param webserver Configuration of webserver for serving files from the target directory
 * @param fields List of fields for placeholder substitutions
 * @param tasks List of build tasks
 */
case class AppConfigImpl(
  source: String = "source",
  target: String = "target",
  webserver: WebServerConfig = WebServerConfig(),
  fields: List[NameValueConfig] = Nil,
  actions: List[ActionConfig] = Nil,
  tasks: List[TaskConfig]) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    ConfigUtil.getString(config, prefix + ".source", "source"),
    ConfigUtil.getString(config, prefix + ".target", "target"),
    AppConfigImpl.getWebServerConfig(config, prefix + ".webserver"),
    AppConfigImpl.getNameValueConfig(config, prefix + ".fields"),
    AppConfigImpl.getActionsConfig(config, prefix + ".actions"),
    AppConfigImpl.getTasksConfig(config, prefix + ".tasks"))
}

/**
 * Statics for AppConfigImpl
 */
object AppConfigImpl extends Logger {

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
   * Returns the list of `ActionConfig`. If not defined, an error is returned
   */
  def getActionsConfig(config: Config, name: String): List[ActionConfig] = {
    try {
      val l = config.getConfigList(name)
      val lb = new ListBuffer[ActionConfig]
      l.foreach(cfg => lb.append(new ActionConfig(cfg)))
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
 * Configuration for an action
 *
 * @param name Unique identifier for the action
 * @param actorName Name of actor in actor system to call to process this task
 * @param defaults Default settings that can be override by a specific task
 */
case class ActionConfig(
  name: String,
  actorName: String,
  defaults: List[NameValueConfig] = Nil) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config) = this(
    config.getString("name"),
    config.getString("actorName"),
    AppConfigImpl.getNameValueConfig(config, "defaults"))
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
 * @param action Name of action to perform. For example `javascript`
 * @param parameters Parameters for the action. Overrides the defaults for the action
 */
case class TaskConfig(
  name: String,
  profile: List[String] = Nil,
  source: String,
  target: String,
  exclude: List[String] = Nil,
  watch: Boolean = true,
  action: String,
  parameters: List[NameValueConfig] = Nil) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config) = this(
    config.getString("name"),
    ConfigUtil.getListString(config, "profile"),
    config.getString("source"),
    config.getString("target"),
    ConfigUtil.getListString(config, "exclude"),
    ConfigUtil.getBoolean(config, "watch", true),
    config.getString("action"),
    AppConfigImpl.getNameValueConfig(config, "parameters"))
}


