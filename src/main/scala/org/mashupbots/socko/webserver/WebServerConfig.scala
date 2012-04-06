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
package org.mashupbots.socko.webserver

import java.io.File
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import com.typesafe.config.Config

/**
 * Web server configuration
 *
 * @param serverName Human friendly name of this server. Helpful in error messages
 * @param hostname Hostname or IP address to bind. `0.0.0.0` will bind to all addresses.
 * 	You can also specify comma separated hostnames/ip address. E.g. `localhost,192.168.1.1`
 * @param port Port to bind to. Defaults to `8888`.
 * @param sslConfig SSL configuration. If None, then SSL will not be turned on.
 * @param processingConfig HTTP request processing configuration
 */
case class WebServerConfig(
  serverName: String = "WebServer",
  hostname: String = "localhost",
  port: Int = 8888,
  sslConfig: Option[SslConfig] = None,
  processingConfig: ProcessingConfig = new ProcessingConfig()) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    config.getString(prefix + ".server-name"),
    config.getString(prefix + ".hostname"),
    config.getInt(prefix + ".port"),
    WebServerConfig.getOptionalSslConfig(config, prefix + ".ssl-config"),
    WebServerConfig.getProcessingConfig(config, prefix + ".processing-config"))

  /**
   * Validate current configuration settings
   */
  def validate() = {
    if (serverName == null || serverName.isEmpty) {
      throw new IllegalArgumentException("server name must be specified")
    }

    if (hostname == null || hostname.isEmpty) {
      throw new IllegalArgumentException("hostname must be specified")
    }
    if (port <= 0) {
      throw new IllegalArgumentException("port must be specified and > 0")
    }

    if (sslConfig.isDefined) {
      if (sslConfig.get.keyStoreFile == null) {
        throw new IllegalArgumentException("key store file must be specified")
      }
      if (!sslConfig.get.keyStoreFile.exists) {
        throw new IllegalArgumentException("key store file does not exist")
      }
      if (!sslConfig.get.keyStoreFile.isFile) {
        throw new IllegalArgumentException("key store file is not a file")
      }
      if (sslConfig.get.keyStorePassword == null || sslConfig.get.keyStorePassword == "") {
        throw new IllegalArgumentException("key store password must be specified")
      }

      if (sslConfig.get.trustStoreFile.isDefined) {
        if (sslConfig.get.trustStoreFile == null || sslConfig.get.trustStoreFile.get == null) {
          throw new IllegalArgumentException("trust store file must be specified")
        }
        if (!sslConfig.get.trustStoreFile.get.exists) {
          throw new IllegalArgumentException("trust store file does not exist")
        }
        if (!sslConfig.get.trustStoreFile.get.isFile) {
          throw new IllegalArgumentException("trust store file is not a file")
        }
        if (sslConfig.get.trustStorePassword == null ||
          sslConfig.get.trustStorePassword.isEmpty ||
          sslConfig.get.trustStorePassword.get == null ||
          sslConfig.get.trustStorePassword.get == "") {
          throw new IllegalArgumentException("trust store password must be specified")
        }
      }
    }

    if (processingConfig == null) {
      throw new IllegalArgumentException("processing config must be specified")
    }
    if (processingConfig.maxLengthInMB <= 0) {
      throw new IllegalArgumentException("processing config maximum length in MB must be specified and > 0")
    }

  }
}

/**
 * SSL Configuration
 *
 * @param keyStoreFile Path to server private key store file (server certificates)
 * @param keyStorePassword Password to access server private key store file.
 * @param trustStoreFile Path to key store file for trusted remote public keys (client certificates).
 * 	This is optional.
 * @param trustStorePassword Password to access the key store for trusted remote public keys (client certificates).
 * 	This is optional.
 */
case class SslConfig(
  keyStoreFile: File,
  keyStorePassword: String,
  trustStoreFile: Option[File],
  trustStorePassword: Option[String]) {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    new File(config.getString(prefix + ".key-store-file")),
    config.getString(prefix + ".key-store-password"),
    WebServerConfig.getOptionalFile(config, prefix + ".trust-store-file"),
    WebServerConfig.getOptionalString(config, prefix + ".trust-store-password"))

}

/**
 * HTTP Request configuration
 *
 * @param maxLengthInMB Maximum size of HTTP request in megabytes. Defaults to 4MB.
 * @param aggreateChunks Flag to indicate if we want to aggregate chunks. If `false`, your processor actors must be
 *  able to handle `HttpChunkProcessingContext`
 */
case class ProcessingConfig(
  maxLengthInMB: Int = 4,
  aggreateChunks: Boolean = true) {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    config.getInt(prefix + ".max-length-in-mb"),
    config.getBoolean(prefix + ".aggreate-chunks"))

  def this() = this(4, true)

}


/**
 * Methods for reading configuration
 */
object WebServerConfig {

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
   * Returns the defined `ProcessingConfig`. If not defined, then the default `ProcessingConfig` is returned.
   */
  def getProcessingConfig(config: Config, name: String): ProcessingConfig = {
    try {
      val v = config.getConfig(name)
      if (v == null) {
        new ProcessingConfig()
      } else {
        new ProcessingConfig(config, name)
      }
    } catch {
      case _ => new ProcessingConfig()
    }
  }

  /**
   * Returns the defined `SslConfig`. If not defined, `None` is returned.
   */
  def getOptionalSslConfig(config: Config, name: String): Option[SslConfig] = {
    try {
      val v = config.getConfig(name)
      if (v == null) {
        None
      } else {
        Some(new SslConfig(config, name))
      }
    } catch {
      case _ => None
    }
  }

}

