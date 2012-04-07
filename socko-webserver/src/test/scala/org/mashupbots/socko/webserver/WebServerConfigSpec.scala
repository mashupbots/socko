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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.mashupbots.socko.context.EndPoint
import org.scalatest.GivenWhenThen
import java.io.File
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

@RunWith(classOf[JUnitRunner])
class WebServerConfigSpec extends WordSpec with ShouldMatchers with GivenWhenThen {

  val aDirectory = new File("/tmp")

  val aFileNotFound = new File("/tmp/notexist")

  val aFile = new File("/tmp/WebServerConfigSpec.txt");
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
      checkForIllegalArgumentException(WebServerConfig(null), "server name")
      checkForIllegalArgumentException(WebServerConfig(""), "server name")
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
        WebServerConfig(sslConfig = Some(SslConfig(null, null, null, null))), "key store file")

      when("keystore file a directory and not a file")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aDirectory, null, null, null))), "key store file")

      when("keystore file does not exist")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFileNotFound, null, null, null))), "key store file")
    }

    "throw Exception if keystore password is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, null, null, null))), "key store password")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "", null, null))), "key store password")
    }

    "throw Exception if truststore file is invalid" in {
      when("truststore file not specified")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(null), null))), "trust store file")

      when("truststore file a directory and not a file")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aDirectory), null))), "trust store file")

      when("truststore file does not exist")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFileNotFound), null))), "trust store file")
    }

    "throw Exception if truststore password is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFile), null))), "trust store password")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFile), Some(null)))), "trust store password")
      checkForIllegalArgumentException(
        WebServerConfig(sslConfig = Some(SslConfig(aFile, "pw", Some(aFile), Some("")))), "trust store password")
    }

    "throw Exception if ProcessingConfig is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(processingConfig = null), "processing config")
    }

    "throw Exception if ProcessingConfig.maxLengthInMB is invalid" in {
      checkForIllegalArgumentException(
        WebServerConfig(processingConfig = ProcessingConfig(0, false)), "processing config")
    }

    "load from Akka Config" in {
      val actorConfig = "barebones-webserver {\n" +
        "  server-name=BareBonesTest\n" +
        "  hostname=\"192.168.0.1\"\n" +
        "  port=9999\n" +
        "}\n" +
        "all-config-webserver {\n" +
        "  server-name = allTest\n" +
        "  hostname = localhost\n" +
        "  port=10000\n" +
        "  ssl-config {\n" +
        "    key-store-file=/tmp/ks.dat\n" +
        "    key-store-password=kspwd\n" +
        "    trust-store-file=/tmp/ts.dat\n" +
        "    trust-store-password=tspwd\n" +
        "  }\n" +
        "  processing-config {\n" +
        "    max-length-in-mb=10\n" +
        "    aggreate-chunks=false\n" +
        "  }\n" +
        "}"

      val actorSystem = ActorSystem("WebServerConfigSpec", ConfigFactory.parseString(actorConfig))
      
      val barebones = BareBonesWebServerConfig(actorSystem)
      barebones.serverName should equal("BareBonesTest")
      barebones.hostname should equal("192.168.0.1")
      barebones.port should equal(9999)
      barebones.sslConfig should equal(None)
      barebones.processingConfig.maxLengthInMB should be(4)
      barebones.processingConfig.aggreateChunks should be(true)
      
      val all = AllWebServerConfig(actorSystem)
      all.serverName should equal("allTest")
      all.hostname should equal("localhost")
      all.port should equal(10000)
      all.sslConfig.get.keyStoreFile.getCanonicalPath should equal("/tmp/ks.dat")
      all.sslConfig.get.keyStorePassword should equal("kspwd")
      all.sslConfig.get.trustStoreFile.get.getCanonicalPath should equal("/tmp/ts.dat")
      all.sslConfig.get.trustStorePassword.get should equal("tspwd")
      all.processingConfig.maxLengthInMB should be(10)
      all.processingConfig.aggreateChunks should be(false)
      
      actorSystem.shutdown()
    }
  }
}

object BareBonesWebServerConfig extends ExtensionId[WebServerConfig] with ExtensionIdProvider {
  override def lookup = BareBonesWebServerConfig
  override def createExtension(system: ExtendedActorSystem) =
    new WebServerConfig(system.settings.config, "barebones-webserver")
}

object AllWebServerConfig extends ExtensionId[WebServerConfig] with ExtensionIdProvider {
  override def lookup = AllWebServerConfig
  override def createExtension(system: ExtendedActorSystem) =
    new WebServerConfig(system.settings.config, "all-config-webserver")
}

