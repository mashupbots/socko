//
// Copyright 2012 Vibul Imtarnasan and David Bolton.
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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.mashupbots.socko.context.EndPoint
import org.scalatest.GivenWhenThen
import java.io.File
import org.mashupbots.socko.WebServerConfig
import org.mashupbots.socko.ProcessingConfig
import org.mashupbots.socko.SslConfig

@RunWith(classOf[JUnitRunner])
class WebServerConfigSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  val aDirectory = new File("/tmp")

  val aFileNotFound = new File("/tmp/notexist")

  val aFile = new File("/tmp/file.txt");
  private val out = new java.io.FileWriter(aFile)
  out.write("test")
  out.close

  def checkForIllegalArgumentException(cfg: WebServerConfig, paramName: String): Unit = {
    val ex = intercept[IllegalArgumentException] {
      cfg.validate()
    }
    assert(ex.getMessage.contains(paramName),
      "'" + paramName + "' does not appear in the error message: " + ex.getMessage)
  }

  "WebServerConfig" should {

    "load with defaults" in {
      WebServerConfig().validate()
    }

    "validate with no SSL configuration" in {
      WebServerConfig("test", "0.0.0.0", 80, None, ProcessingConfig()).validate()
    }

    "validate with server side (keystore) SSL configuration" in {
      WebServerConfig(
        "test", "0.0.0.0", 80, Some(SslConfig(aFile, "test", None, None)), ProcessingConfig()).validate()
    }

    "validate with client (truststore) and server side (keystore) SSL configuration" in {
      WebServerConfig(
        "test", "0.0.0.0", 80, Some(SslConfig(aFile, "test", Some(aFile), Some("test"))), ProcessingConfig()).validate()
    }

    "throw Exception when server name is not supplied" in {
      checkForIllegalArgumentException(WebServerConfig(null), "serverName")
      checkForIllegalArgumentException(WebServerConfig(""), "serverName")
    }

    "throw Exception when host name is not supplied" in {
      checkForIllegalArgumentException(WebServerConfig(hostname = null), "hostname")
      checkForIllegalArgumentException(WebServerConfig(hostname = ""), "hostname")
    }

    "throw Exception if port is invalid" in {
      WebServerConfig(port = 100)

      when("port is 0")
      checkForIllegalArgumentException(WebServerConfig(port = 0), "port")

      when("port is negative")
      checkForIllegalArgumentException(WebServerConfig(port = -100), "port")
    }

    "throw Exception if keystore file is invalid" in {
      when("keystore file not specified")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(null, null, null, null))), "keyStoreFile")

      when("keystore file a directory and not a file")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aDirectory, null, null, null))), "keyStoreFile")

      when("keystore file does not exist")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFileNotFound, null, null, null))), "keyStoreFile")
    }

    "throw Exception if keystore password is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, null, null, null))), "keyStorePassword")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "", null, null))), "keyStorePassword")
    }

    "throw Exception if truststore file is invalid" in {
      when("truststore file not specified")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(null), null))), "trustStoreFile")

      when("truststore file a directory and not a file")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aDirectory), null))), "trustStoreFile")

      when("truststore file does not exist")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFileNotFound), null))), "trustStoreFile")
    }

    "throw Exception if truststore password is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFile), null))), "trustStorePassword")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFile), Some(null)))), "trustStorePassword")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFile), Some("")))), "trustStorePassword")
    }

    "throw Exception if ProcessingConfig is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(processingConfig = null), "processingConfig")
    }

    "throw Exception if ProcessingConfig.maxLengthInMB is invalid" in {
      checkForIllegalArgumentException(
        WebServerConfig(processingConfig = ProcessingConfig(0, false)), "processingConfig")
    }
    
  }
}