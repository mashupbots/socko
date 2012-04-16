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
import org.mashupbots.socko.utils.Logger
import com.typesafe.config.ConfigException

/**
 * Web server configuration
 *
 * The configuration can be optionally loaded from Akka's application.conf` file.
 *
 * The following configuration file:
 * {{{
 *   akka-config-example {
 *     server-name=AkkaConfigExample
 *     hostname=localhost
 *     port=9000
 *   }
 * }}}
 *
 * can be loaded as follows:
 * {{{
 *   object MyWebServerConfig extends ExtensionId[WebServerConfig] with ExtensionIdProvider {
 *     override def lookup = MyWebServerConfig
 *     override def createExtension(system: ExtendedActorSystem) =
 *       new WebServerConfig(system.settings.config, "akka-config-example")
 *   }
 *
 *   val myWebServerConfig = MyWebServerConfig(actorSystem)
 *   val webServer = new WebServer(myWebServerConfig, routes)
 *   webServer.start()
 * }}}
 *
 * @param serverName Human friendly name of this server. Defaults to `WebServer`.
 * @param hostname Hostname or IP address to bind. `0.0.0.0` will bind to all addresses.
 * 	You can also specify comma separated hostnames/ip address like `localhost,192.168.1.1`.
 *  Defaults to `localhost`.
 * @param port Port to bind to. Defaults to `8888`.
 * @param sslConfig SSL protocol configuration. If `None`, then SSL will not be turned on.
 *  Defaults to `None`.
 * @param httpConfig HTTP protocol configuration. Default to a and instance of 
 *  [[org.mashupbots.socko.webserver.HttpConfig]] with default settings.
 */
case class WebServerConfig(
  serverName: String = "WebServer",
  hostname: String = "localhost",
  port: Int = 8888,
  sslConfig: Option[SslConfig] = None,
  httpConfig: HttpConfig = HttpConfig()) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    config.getString(prefix + ".server-name"),
    config.getString(prefix + ".hostname"),
    config.getInt(prefix + ".port"),
    WebServerConfig.getOptionalSslConfig(config, prefix + ".ssl-config"),
    WebServerConfig.getHttpConfig(config, prefix + ".http-config"))

  /**
   * Validate current configuration settings. Throws an exception if configuration has errors.
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

    if (httpConfig == null) {
      throw new IllegalArgumentException("HTTP configuration must be specified")
    }
    if (httpConfig.maxLengthInMB <= 0) {
      throw new IllegalArgumentException("HTTP configuration, maximum length in MB, must be specified and > 0")
    }
    if (httpConfig.maxInitialLineLength <= 0) {
      throw new IllegalArgumentException("HTTP configuration, maximum initial line length, must be > 0")
    }
    if (httpConfig.maxHeaderSizeInBytes < 0) {
      throw new IllegalArgumentException("HTTP configuration, maximum header size, must be > 0")
    }
    if (httpConfig.maxChunkSizeInBytes < 0) {
      throw new IllegalArgumentException("HTTP configuration, maximum chunk size, must be > 0")
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
 * HTTP protocol handling configuration
 *
 * @param maxLengthInMB Maximum size of HTTP request in megabytes. Defaults to 4MB.
 * @param maxInitialLineLength Maximum size the initial line. Defaults to 4096 characters.
 * @param maxHeaderSizeInBytes Maximum size of HTTP headers. Defaults to 8192 bytes.
 * @param maxChunkSizeInBytes Maximum size of HTTP chunks. Defaults to 8192 bytes.
 * @param aggreateChunks Flag to indicate if we want to aggregate chunks. If `false`, your processor actors must be
 *  able to handle `HttpChunkProcessingContext`
 */
case class HttpConfig(
  maxLengthInMB: Int = 4,
  maxInitialLineLength: Int = 4096,
  maxHeaderSizeInBytes: Int = 8192,
  maxChunkSizeInBytes: Int = 8192,
  aggreateChunks: Boolean = true) {

  val maxLengthInBytes = maxLengthInMB * 1024 * 1024

  /**
   * Read configuration from AKKA's `application.conf`. Supply default values to use if setting not present
   */
  def this(config: Config, prefix: String) = this(
    WebServerConfig.getInt(config, prefix + ".max-length-in-mb", 4),
    WebServerConfig.getInt(config, prefix + ".max-initial-line-length", 4096),
    WebServerConfig.getInt(config, prefix + ".max-header-size-in-bytes", 8192),
    WebServerConfig.getInt(config, prefix + ".max-chunk-size-in-bytes", 8192),
    WebServerConfig.getBoolean(config, prefix + ".aggregate-chunks", true))
}

/**
 * Methods for reading configuration from Akka.
 */
object WebServerConfig extends Logger {

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
   * Returns the defined `ProcessingConfig`. If not defined, then the default `ProcessingConfig` is returned.
   */
  def getHttpConfig(config: Config, name: String): HttpConfig = {
    try {
      val v = config.getConfig(name)
      if (v == null) {
        new HttpConfig()
      } else {
        new HttpConfig(config, name)
      }
    } catch {
      case ex: ConfigException.Missing => {
        new HttpConfig()
      }
      case ex => {
        log.error("Error parsing processing config. Defaults will be used.", ex)
        new HttpConfig()
      }
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
      case ex: ConfigException.Missing => {
        None
      }
      case ex => {
        log.error("Error parsing SSL config. SSL is turned off.", ex)
        None
      }
    }
  }

}

