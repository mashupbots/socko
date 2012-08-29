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
 * @param src Path to source folder. Maybe full path or relative to the directory of the configuration file
 * @param target Path to target folder when output files will be stored. Maybe full path or relative to the directory 
 *   of the configuration file
 * @param webserver Configuration of webserver for serving files from the target directory
 * @param tasks List of build tasks
 */
case class AppConfigImpl(
  src: File = new File("src"),
  target: File = new File("target"),
  webserver: WebServerConfig = new WebServerConfig(),
  tasks: List[TaskConfig]) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    AppConfigImpl.getFile(config, prefix + ".src", "src"),
    AppConfigImpl.getFile(config, prefix + ".target", "target"),
    AppConfigImpl.getWebServerConfig(config, prefix + ".webserver"),
    AppConfigImpl.getTasksConfig(config, prefix + ".tasks"))
}

/**
 * Configuration for a build task
 * 
 * @param id Unique identifier for the task
 */
case class TaskConfig(
  id: String) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config) = this(
    config.getString("id"))
}

/**
 * Statics for AppConfigImpl
 */
object AppConfigImpl extends Logger {
  /**
   * Returns a `File` setting
   */
  def getFile(config: Config, name: String, defaultValue: String): File = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        new File(defaultValue)
      } else {
        new File(v)
      }
    } catch {
      case _ => new File(defaultValue)
    }
  }

  /**
   * Returns the defined `WebServerConfig`. If not defined, then the default `WebServerConfig` is returned.
   */
  def getWebServerConfig(config: Config, name: String): WebServerConfig = {
    try {
      val v = config.getConfig(name)
      if (v == null) {
        new WebServerConfig()
      } else {
        new WebServerConfig(config, name)
      }
    } catch {
      case ex: ConfigException.Missing => {
        new WebServerConfig()
      }
      case ex => {
        log.info("Error parsing WebServerConfig. Defaults will be used.")
        log.debug("Exception", ex)
        new WebServerConfig()
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