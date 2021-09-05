//
// Copyright 2012-2013 Vibul Imtarnasan, David Bolton and Socko contributors.
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
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit

/**
 * A utility class for reading AKKA configuration
 */
object ConfigUtil {

  /**
   * Returns an file configuration value. It is assumed that the value of the configuration is the full
   * path to a file or directory.
   */
  def getFile(config: Config, name: String, defaultValue: File): File = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        defaultValue
      } else {
        new File(v)
      }
    } catch {
      case _: Throwable => defaultValue
    }
  }
  
  /**
   * Returns an optional file configuration value. It is assumed that the value of the configuration is the full
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
      case _: Throwable => None
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
      case _: Throwable => defaultValue
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
      case _: Throwable => None
    }
  }

  /**
   * Returns the specified comma separated value setting as an sequence of string values. 
   * If setting not specified, then the default is returned.
   */
  def getCSV(config: Config, name: String, defaultValue: Seq[String]): Seq[String] = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        defaultValue
      } else {
        v.split(",").map(s => s.trim()).toSeq
      }
    } catch {
      case _: Throwable => defaultValue
    }
  }

  /**
   * Returns an optional comma separated string configuration value
   */
  def getOptionalCSV(config: Config, name: String): Option[Seq[String]] = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        None
      } else {
        Some(v.split(",").map(s => s.trim()).toSeq)
      }
    } catch {
      case _: Throwable => None
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
      case _: Throwable => defaultValue
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
      case _: Throwable => None
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
      case _: Throwable => defaultValue
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
      case _: Throwable => None
    }
  }

  /**
   * Returns the specified setting as an string. If setting not specified, then the default is returned.
   */
  def getListString(config: Config, name: String): List[String] = {
    try {
      val v = config.getStringList(name).asScala
      if (v == null || v.length == 0) {
        Nil
      } else {
        v.toList
      }
    } catch {
      case _: Throwable => Nil
    }
  }
  
  
  /**
   * Returns the specified setting as a Duration. If setting not specified, then the default is returned.
   */
  def getDuration(config: Config, name: String, defaultValue: Duration): Duration = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        defaultValue
      } else {
        Duration(config.getDuration(name, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
      }
    } catch {
      case _: Throwable => defaultValue
    }
  }

  /**
   * Returns the specified setting as a Duration. `None` is returned if setting not specified
   */
  def getOptionalDuration(config: Config, name: String): Option[Duration] = {
    try {
      val v = config.getString(name)
      if (v == null || v == "") {
        None
      } else {
        Some(Duration(config.getDuration(name, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))
      }
    } catch {
      case _: Throwable => None
    }
  }
  
}