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

import java.io.File

import scala.collection.JavaConversions.asScalaBuffer

import com.typesafe.config.Config

/**
 * A utility class for reading AKKA configuration
 */
object ConfigUtil {

  /**
   * Returns an optional file configuration value. It is assumed that the value of the configuration name is the full
   * path to a file or directory.
   */
  def getOptionalFile(config: Config, name: String): Option[File] = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        None
      } else {
        Some(new File(v))
      }
    } catch {
      case _ => None
    }
  }

  /**
   * Returns the specified setting as an string. If setting not specified, then the default is returned.
   */
  def getString(config: Config, name: String, defaultValue: String): String = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        defaultValue
      } else {
        v
      }
    } catch {
      case _ => defaultValue
    }
  }

  /**
   * Returns an optional string configuration value
   */
  def getOptionalString(config: Config, name: String): Option[String] = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        None
      } else {
        Some(v)
      }
    } catch {
      case _ => None
    }
  }

  /**
   * Returns the specified setting as an integer. If setting not specified, then the default is returned.
   */
  def getInt(config: Config, name: String, defaultValue: Int): Int = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        defaultValue
      } else {
        config.getInt(name)
      }
    } catch {
      case _ => defaultValue
    }
  }

  /**
   * Returns the specified setting as an integer. If setting not specified, then the default is returned.
   */
  def getOptionalInt(config: Config, name: String): Option[Int] = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        None
      } else {
        Some(config.getInt(name))
      }
    } catch {
      case _ => None
    }
  }

  /**
   * Returns the specified setting as a boolean. If setting not specified, then the default is returned.
   */
  def getBoolean(config: Config, name: String, defaultValue: Boolean): Boolean = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        defaultValue
      } else {
        config.getBoolean(name)
      }
    } catch {
      case _ => defaultValue
    }
  }

  /**
   * Returns the specified setting as a boolean. `None` is returned if setting not specified
   */
  def getOptionalBoolean(config: Config, name: String): Option[Boolean] = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        None
      } else {
        Some(config.getBoolean(name))
      }
    } catch {
      case _ => None
    }
  }

  /**
   * Returns the specified setting as an string. If setting not specified, then the default is returned.
   */
  def getListString(config: Config, name: String): List[String] = {
    try {
      val v = config.getStringList(name)
      if (v == null || v.length == 0) {
        Nil
      } else {
        v.toList
      }
    } catch {
      case _ => Nil
    }
  }
}