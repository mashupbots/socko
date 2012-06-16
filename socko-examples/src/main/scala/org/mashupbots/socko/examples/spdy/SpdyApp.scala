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
package org.mashupbots.socko.examples.spdy

import java.io.File
import java.io.FileOutputStream

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers.StaticContentHandler
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.handlers.StaticFileRequest
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.IOUtil
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes.GET
import org.mashupbots.socko.routes.HttpRequest
import org.mashupbots.socko.routes.Path
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.webserver.HttpConfig
import org.mashupbots.socko.webserver.SslConfig
import org.mashupbots.socko.webserver.WebLogConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import com.typesafe.config.ConfigFactory

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig

/**
 * This example is used for testing SPDY.
 * 
 * To run this example you need to:
 *  - use JDK 7
 *  - add `-Xbootclasspath/p:/path/to/sbt/npn-boot-VERSION.jar` to your JVM start up parameter
 *  - browse using Chrome
 *  
 * For an example, see `/socko/sbt/sbt` file.
 */
object SpdyApp extends Logger {

  val contentDir = createTempDir("content_")
  val tempDir = createTempDir("temp_")

  StaticContentHandlerConfig.rootFilePaths = Seq(contentDir.getAbsolutePath)
  StaticContentHandlerConfig.tempDir = tempDir
  //StaticContentHandlerConfig.serverCacheMaxFileSize = 0

  //
  // STEP #1 - Define Actors and Start Akka
  //
  val actorConfig = """
	my-pinned-dispatcher {
	  type=PinnedDispatcher
	  executor=thread-pool-executor
	}
	my-dispatcher {
	  # Dispatcher is the name of the event-based dispatcher
	  type = Dispatcher
	  # What kind of ExecutionService to use
	  executor = "thread-pool-executor"
	  # Configuration for the fork join pool
	  thread-pool-executor {
	    # Min number of threads to cap factor-based parallelism number to
	    parallelism-min = 25
	    # Parallelism (threads) ... ceil(available processors * factor)
	    parallelism-factor = 2.0
	    # Max number of threads to cap factor-based parallelism number to
	    parallelism-max = 6
	  }
	  # Throughput defines the maximum number of messages to be
	  # processed per actor before the thread jumps to the next actor.
	  # Set to 1 for as fair as possible.
	  throughput = 100
	} 
    akka {
	  event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
	  loglevel=DEBUG
	  actor {
	    deployment {
	      /static-file-router {
	        router = round-robin
	        nr-of-instances = 20
	      }
	    }
	  }
	}"""

  val actorSystem = ActorSystem("BenchmarkActorSystem", ConfigFactory.parseString(actorConfig))
  val staticContentHandlerRouter = actorSystem.actorOf(Props[StaticContentHandler]
    .withRouter(FromConfig()).withDispatcher("my-dispatcher"), "static-file-router")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case HttpRequest(request) => request match {
      case GET(Path("/small.html")) => {
        staticContentHandlerRouter ! new StaticFileRequest(request, new File(contentDir, "small.html"))
      }
      case GET(Path("/medium.txt")) => {
        staticContentHandlerRouter ! new StaticFileRequest(request, new File(contentDir, "medium.txt"))
      }
      case GET(Path("/big.txt")) => {
        staticContentHandlerRouter ! new StaticFileRequest(request, new File(contentDir, "big.txt"))
      }
      case GET(Path("/dynamic")) => {
        actorSystem.actorOf(Props[DynamicHandler].withDispatcher("my-dispatcher")) ! request
      }
      case GET(Path("/favicon.ico")) => {
        request.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }
  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    // Create content
    createContent(contentDir)

    // Start web server
    val keyStoreFile = new File(contentDir, "testKeyStore")
    val keyStoreFilePassword = "password"
    val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None)
    val httpConfig = HttpConfig(spdyEnabled = true)
    val webServerConfig = WebServerConfig(hostname="0.0.0.0", webLog = Some(WebLogConfig()), ssl = Some(sslConfig), http = httpConfig)
    val webServer = new WebServer(webServerConfig, routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run {
        webServer.stop()
        contentDir.delete()
        tempDir.delete()
      }
    })
    webServer.start()

    System.out.println("Content directory is " + contentDir.getAbsolutePath)
    System.out.println("87 bytes File   : https://localhost:8888/small.html")
    System.out.println("200K File       : https://localhost:8888/medium.txt")
    System.out.println("1MB File        : https://localhost:8888/big.txt")
    System.out.println("Dynamic Content : https://localhost:8888/dynamic")
    System.out.println("")
    System.out.println("Make sure that you:")
    System.out.println("1. Are using JDK 7")
    System.out.println("2. Have added `-Xbootclasspath/p:/path/to/sbt/npn-boot-VERSION.jar` to your JVM start up parameter")
    System.out.println("   Refer to /socko/sbt/sbt for an example")
    System.out.println("3. Browse using Chrome")
    System.out.println("")
    System.out.println("Check your SPDY session at chrome://net-internals/#spdy")
    System.out.println("")
  }

  /**
   * Returns a newly created temp directory
   *
   * @param namePrefix Prefix to use on the directory name
   * @returns Newly created directory
   */
  private def createTempDir(namePrefix: String): File = {
    val d = File.createTempFile(namePrefix, "")
    d.delete()
    d.mkdir()
    d
  }

  /**
   * Delete the specified directory and all sub directories
   *
   * @param dir Directory to delete
   */
  private def deleteTempDir(dir: File) {
    if (dir.exists()) {
      val files = dir.listFiles()
      files.foreach(f => {
        if (f.isFile) {
          f.delete()
        } else {
          deleteTempDir(dir)
        }
      })
    }
    dir.delete()
  }

  /**
   * Create files for downloading
   */
  private def createContent(dir: File) {
    val buf = new StringBuilder()

    // medium.txt - 200K file
    buf.setLength(0)
    for (i <- 0 until (1024 * 200)) {
      buf.append('a')
    }

    val mediumFile = new File(dir, "medium.txt")
    val out = new FileOutputStream(mediumFile)
    out.write(buf.toString.getBytes(CharsetUtil.UTF_8))
    out.close()

    // big.txt - 1MB file
    buf.setLength(0)
    for (i <- 0 until (1024 * 1024)) {
      buf.append('a')
    }

    val bigFile = new File(dir, "big.txt")
    val out2 = new FileOutputStream(bigFile)
    out2.write(buf.toString.getBytes(CharsetUtil.UTF_8))
    out2.close()

    // copy over small foo.html (same file used by vertx)
    val fooFile = new File(dir, "small.html")
    val out3 = new FileOutputStream(fooFile)
    out3.write(IOUtil.readResource("foo.html"))
    out3.close()
    
    // copy over keystore
    val ksFile = new File(dir, "testKeyStore")
    val out4 = new FileOutputStream(ksFile)
    out4.write(IOUtil.readResource("testKeyStore"))
    out4.close()
  }
}
