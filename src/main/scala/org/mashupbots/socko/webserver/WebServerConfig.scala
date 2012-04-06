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
  processingConfig: ProcessingConfig = new ProcessingConfig()) {

  def validate() = {
    if (serverName == null || serverName.isEmpty) {
      throw new IllegalArgumentException("serverName must be specified")
    }

    if (hostname == null || hostname.isEmpty) {
      throw new IllegalArgumentException("hostname must be specified")
    }
    if (port <= 0) {
      throw new IllegalArgumentException("port must be specified and > 0")
    }

    if (sslConfig.isDefined) {
      if (sslConfig.get.keyStoreFile == null) {
        throw new IllegalArgumentException("keyStoreFile must be specified")
      }
      if (!sslConfig.get.keyStoreFile.exists) {
        throw new IllegalArgumentException("keyStoreFile does not exist")
      }
      if (!sslConfig.get.keyStoreFile.isFile) {
        throw new IllegalArgumentException("keyStoreFile is not a file")
      }
      if (sslConfig.get.keyStorePassword == null || sslConfig.get.keyStorePassword == "") {
        throw new IllegalArgumentException("keyStorePassword must be specified")
      }

      if (sslConfig.get.trustStoreFile.isDefined) {
        if (sslConfig.get.trustStoreFile == null || sslConfig.get.trustStoreFile.get == null) {
          throw new IllegalArgumentException("trustStoreFile must be specified")
        }
        if (!sslConfig.get.trustStoreFile.get.exists) {
          throw new IllegalArgumentException("trustStoreFile does not exist")
        }
        if (!sslConfig.get.trustStoreFile.get.isFile) {
          throw new IllegalArgumentException("trustStoreFile is not a file")
        }
        if (sslConfig.get.trustStorePassword == null ||
            sslConfig.get.trustStorePassword.isEmpty ||
            sslConfig.get.trustStorePassword.get == null ||
            sslConfig.get.trustStorePassword.get == "") {
          throw new IllegalArgumentException("trustStorePassword must be specified")
        }
      }
    }

    if (processingConfig == null) {
      throw new IllegalArgumentException("processingConfig must be specified")
    }
    if (processingConfig.maxLengthInMB <= 0) {
      throw new IllegalArgumentException("processingConfig maxLengthInMB must be specified and > 0")
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

  def this() = this(4, true)
}

