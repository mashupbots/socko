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
package org.mashupbots.socko.examples.benchmark

import java.io.File
import java.io.FileOutputStream

import org.mashupbots.socko.context.HttpResponseStatus
import org.mashupbots.socko.processors.StaticFileProcessor
import org.mashupbots.socko.processors.StaticFileRequest
import org.mashupbots.socko.routes._
import org.mashupbots.socko.utils.CharsetUtil
import org.mashupbots.socko.utils.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.FromConfig

/**
 * This example is used for benchmarking
 *  - http://localhost:8888/test.html is used for small static file (36 bytes)
 *  - http://localhost:8888/data.dat is used for big static file (1MB)
 *  - http://localhost:8888/dynamic is used for dynamic content
 */
object BenchmarkApp extends Logger {

  val contentDir = createTempDir("content_")
  val tempDir = createTempDir("temp_")

  //
  // STEP #1 - Define Actors and Start Akka
  //
  // We are going to start StaticFileProcessor actor as a router.
  // There will be 5 instances, each instance having its own thread since there is a lot of blocking IO.
  //
  // FileUploadProcessor will also be started as a router with a PinnedDispatcher since it involves IO.
  //
  val actorConfig = """
	my-pinned-dispatcher {
	  type=PinnedDispatcher
	  executor=thread-pool-executor
	}
	akka {
	  event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
	  loglevel=DEBUG
	  actor {
	    deployment {
	      /static-file-router {
	        router = round-robin
	        nr-of-instances = 100
	      }
	    }
	  }
	}"""

  val actorSystem = ActorSystem("BenchmarkActorSystem", ConfigFactory.parseString(actorConfig))

  val staticFileProcessorRouter = actorSystem.actorOf(Props[StaticFileProcessor]
    .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case HttpRequest(rq) => rq match {
      case GET(Path("/test.html")) => {
        val staticFileRequest = new StaticFileRequest(
          rq,
          contentDir,
          new File(contentDir, "test.html"),
          tempDir)
        staticFileProcessorRouter ! staticFileRequest
      }
      case GET(Path("/data.dat")) => {
        val staticFileRequest = new StaticFileRequest(
          rq,
          contentDir,
          new File(contentDir, "data.dat"),
          tempDir)
        staticFileProcessorRouter ! staticFileRequest
      }
      case GET(Path("/dynamic")) => {
        actorSystem.actorOf(Props[DynamicBenchmarkProcessor]) ! rq
      }
      case GET(Path("/favicon.ico")) => {
        rq.response.write(HttpResponseStatus.NOT_FOUND)
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
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run {
        webServer.stop()
        contentDir.delete()
        tempDir.delete()
      }
    })
    webServer.start()

    System.out.println("Content directory is " + contentDir.getCanonicalPath)
    System.out.println("Small Static File: http://localhost:8888/test.html")
    System.out.println("Big Static File  : http://localhost:8888/data.dat")
    System.out.println("Dynamic Content  : http://localhost:8888/dynamic")
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
    // test.html - 36 byte file
    val buf = new StringBuilder()
    buf.append("<html>\n")
    buf.append("<body>\n")
    buf.append("Hello\n")
    buf.append("</body>\n")
    buf.append("</html>\n")

    val smallFile = new File(dir, "test.html")
    val out = new FileOutputStream(smallFile)
    out.write(buf.toString.getBytes(CharsetUtil.UTF_8))
    out.close()

    // data.dat - 1MB file
    buf.setLength(0)
    for (i <- 0 until (1024 * 1024)) {
      buf.append('a')
    }

    val bigFile = new File(dir, "data.dat")
    val out2 = new FileOutputStream(bigFile)
    out2.write(buf.toString.getBytes(CharsetUtil.UTF_8))
    out2.close()
  }
}
