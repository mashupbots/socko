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
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import org.mashupbots.socko.infrastructure.WebLogFormat

@RunWith(classOf[JUnitRunner])
class WebServerConfigSpec extends WordSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterAll {

  var aDirectory: File = null

  var aFileNotFound: File = null

  var aFile: File = null

  def checkForIllegalArgumentException(cfg: WebServerConfig, paramName: String): Unit = {
    val ex = intercept[IllegalArgumentException] {
      cfg.validate()
    }
    assert(ex.getMessage.contains(paramName),
      "'" + paramName + "' does not appear in the error message: " + ex.getMessage)
  }

  override def beforeAll(configMap: Map[String, Any]) {
    aDirectory = File.createTempFile("ADir_", "")
    aDirectory.delete()
    aDirectory.mkdir()

    aFileNotFound = new File(aDirectory, "notexist.txt")

    aFile = new File("/tmp/WebServerConfigSpec.txt");
    val out = new java.io.FileWriter(aFile)
    out.write("test")
    out.close
  }

  override def afterAll(configMap: Map[String, Any]) {
    if (aDirectory != null) {
      deleteDirectory(aDirectory)
      aDirectory = null
    }
  }

  def deleteDirectory(path: File): Boolean = {
    if (path.exists()) {
      val files = path.listFiles()
      files.foreach(f => {
        if (f.isFile) {
          f.delete()
        } else {
          deleteDirectory(f)
        }
      })
    }
    path.delete()
  }

  "WebServerConfig" should {

    "load with defaults" in {
      WebServerConfig().validate()
    }

    "validate with no SSL configuration" in {
      WebServerConfig("test", "0.0.0.0", 80, None, None, HttpConfig()).validate()
    }

    "validate with server side (keystore) SSL configuration" in {
      WebServerConfig(
        "test", "0.0.0.0", 80, None, Some(SslConfig(aFile, "test", None, None)), HttpConfig()).validate()
    }

    "validate with client (truststore) and server side (keystore) SSL configuration" in {
      WebServerConfig(
        "test", "0.0.0.0", 80, None, Some(SslConfig(aFile, "test", Some(aFile), Some("test"))), HttpConfig()).validate()
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
        WebServerConfig(ssl = Some(SslConfig(null, null, null, null))), "key store file")

      when("keystore file a directory and not a file")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aDirectory, null, null, null))), "key store file")

      when("keystore file does not exist")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFileNotFound, null, null, null))), "key store file")
    }

    "throw Exception if keystore password is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, null, null, null))), "key store password")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, "", null, null))), "key store password")
    }

    "throw Exception if truststore file is invalid" in {
      when("truststore file not specified")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, "pw", Some(null), null))), "trust store file")

      when("truststore file a directory and not a file")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, "pw", Some(aDirectory), null))), "trust store file")

      when("truststore file does not exist")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, "pw", Some(aFileNotFound), null))), "trust store file")
    }

    "throw Exception if truststore password is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, "pw", Some(aFile), null))), "trust store password")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, "pw", Some(aFile), Some(null)))), "trust store password")
      checkForIllegalArgumentException(
        WebServerConfig(ssl = Some(SslConfig(aFile, "pw", Some(aFile), Some("")))), "trust store password")
    }

    "throw Exception if HttpConfig is not supplied" in {
      checkForIllegalArgumentException(
        WebServerConfig(http = null), "HTTP configuration")
    }

    "throw Exception if HttpConfig settings are invalid" in {
      checkForIllegalArgumentException(
        WebServerConfig(http = HttpConfig(0, 0, 0, 0, false)), "HTTP configuration, maximum length in MB")

      checkForIllegalArgumentException(
        WebServerConfig(http = HttpConfig(1, -1, 0, 0, false)), "HTTP configuration, maximum initial line length")

      checkForIllegalArgumentException(
        WebServerConfig(http = HttpConfig(1, 1, -1, 0, false)), "HTTP configuration, maximum header size")

      checkForIllegalArgumentException(
        WebServerConfig(http = HttpConfig(1, 1, 0, -1, false)), "HTTP configuration, maximum chunk size")
    }

    "load from Akka Config" in {

      // *** If you are changing this, review scaladoc of WebServerConfig ***
      val actorConfig = """
		barebones-webserver {
		  server-name=BareBonesTest
		  hostname="192.168.0.1"
		  port=9999
		}
		all-config-webserver {
		  server-name = allTest
		  hostname = localhost
		  port=10000
          web-log {
            custom-actor-path = "akka://my-system/user/web-log-writer"
            format = Extended
          }
		  ssl {
		    key-store-file=/tmp/ks.dat
		    key-store-password=kspwd
		    trust-store-file=/tmp/ts.dat
		    trust-store-password=tspwd
		  }
		  http {
		    max-length-in-mb=10
		    max-initial-line-length=20
		    max-header-size-in-bytes=30
		    max-chunk-size-in-bytes=40
		    aggregate-chunks=false
            min-compressible-content-size-in-bytes=50
            max-compressible-content-size-in-bytes=60
            compressible-content-types=["text/plain", "text/html"]
            spdy=true
		  }
          tcp {
            no-delay=true
            send-buffer-size=1
            receive-buffer-size=2
            keep-alive=true
            reuse-address=true
            so-linger=3
            traffic-class=4
            accept-backlog=5
          }
		}"""
      // *** If you are changing this, review scaladoc of WebServerConfig ***

      val actorSystem = ActorSystem("WebServerConfigSpec", ConfigFactory.parseString(actorConfig))

      val barebones = BareBonesWebServerConfig(actorSystem)
      barebones.serverName should equal("BareBonesTest")
      barebones.hostname should equal("192.168.0.1")
      barebones.port should equal(9999)
      barebones.webLog should be(None)
      barebones.ssl should equal(None)
      barebones.http.maxLengthInMB should be(4)
      barebones.http.aggreateChunks should be(true)
      barebones.http.minCompressibleContentSizeInBytes should be(1024)
      barebones.http.maxCompressibleContentSizeInBytes should be(1024 * 1024)
      barebones.http.compressibleContentTypes.length should be(11)
      barebones.http.spdyEnabled should be(false)
      barebones.tcp.noDelay should be(None)
      barebones.tcp.sendBufferSize should be(None)
      barebones.tcp.receiveBufferSize should be(None)
      barebones.tcp.keepAlive should be(None)
      barebones.tcp.reuseAddress should be(None)
      barebones.tcp.soLinger should be(None)
      barebones.tcp.trafficClass should be(None)
      barebones.tcp.acceptBackLog should be(None)

      val all = AllWebServerConfig(actorSystem)
      all.serverName should equal("allTest")
      all.hostname should equal("localhost")
      all.port should equal(10000)

      all.webLog.get.format should be(WebLogFormat.Extended)
      all.webLog.get.customActorPath.get should be("akka://my-system/user/web-log-writer")

      all.ssl.get.keyStoreFile.getAbsolutePath should equal("/tmp/ks.dat")
      all.ssl.get.keyStorePassword should equal("kspwd")
      all.ssl.get.trustStoreFile.get.getAbsolutePath should equal("/tmp/ts.dat")
      all.ssl.get.trustStorePassword.get should equal("tspwd")

      all.http.maxLengthInMB should be(10)
      all.http.maxLengthInBytes should be(10 * 1024 * 1024)
      all.http.maxInitialLineLength should be(20)
      all.http.maxHeaderSizeInBytes should be(30)
      all.http.maxChunkSizeInBytes should be(40)
      all.http.aggreateChunks should be(false)
      all.http.minCompressibleContentSizeInBytes should be(50)
      all.http.maxCompressibleContentSizeInBytes should be(60)
      all.http.compressibleContentTypes.length should be(2)
      all.http.spdyEnabled should be(true)

      all.tcp.noDelay should be(Some(true))
      all.tcp.sendBufferSize should be(Some(1))
      all.tcp.receiveBufferSize should be(Some(2))
      all.tcp.keepAlive should be(Some(true))
      all.tcp.reuseAddress should be(Some(true))
      all.tcp.soLinger should be(Some(3))
      all.tcp.trafficClass should be(Some(4))
      all.tcp.acceptBackLog should be(Some(5))
      
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

