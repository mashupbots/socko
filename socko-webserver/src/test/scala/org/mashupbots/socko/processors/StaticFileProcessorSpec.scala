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
package org.mashupbots.socko.processors

import java.io.File
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

import org.junit.runner.RunWith
import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.routes.GET
import org.mashupbots.socko.routes.Path
import org.mashupbots.socko.routes.PathSegments
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.utils.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec

import com.typesafe.config.ConfigFactory

import akka.actor.actorRef2Scala
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig

@RunWith(classOf[JUnitRunner])
class StaticFileProcessorSpec
  extends WordSpec with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen with TestHttpClient with Logger {

  val actorConfig = "my-pinned-dispatcher {\n" +
    "  type=PinnedDispatcher\n" +
    "  executor=thread-pool-executor\n" +
    "}\n" +
    "akka {\n" +
    "  event-handlers = [\"akka.event.slf4j.Slf4jEventHandler\"]\n" +
    "  loglevel=DEBUG\n" +
    "  actor {\n" +
    "    deployment {\n" +
    "      /my-router {\n" +
    "        router = round-robin\n" +
    "        nr-of-instances = 5\n" +
    "      }\n" +
    "    }\n" +
    "  }\n" +
    "}"

  var actorSystem: ActorSystem = null
  var webServer: WebServer = null
  val port = 9001
  val path = "http://localhost:" + port + "/"
  var router: ActorRef = null
  var rootDir: File = null
  var tempDir: File = null
  val browserCacheTimeoutSeconds = 60
  val fileLastModifiedCacheTimeoutSeconds = 2

  val routes = Routes({
    case ctx @ GET(Path(PathSegments("files" :: relativePath))) => {
      val request = new StaticFileRequest(
        ctx.asInstanceOf[HttpRequestProcessingContext],
        rootDir,
        new File(rootDir, relativePath.mkString("/", "/", "")),
        tempDir,
        browserCacheTimeoutSeconds,
        fileLastModifiedCacheTimeoutSeconds)
      router ! request
    }
  })

  override def beforeAll(configMap: Map[String, Any]) {
    // Instance akka system
    actorSystem = ActorSystem("StaticFileProcessorSpec", ConfigFactory.parseString(actorConfig))

    // Create root and temp dir
    rootDir = File.createTempFile("Root_", "")
    rootDir.delete()
    rootDir.mkdir()

    tempDir = File.createTempFile("Temp_", "")
    tempDir.delete()
    tempDir.mkdir()

    // Start routers
    router = actorSystem.actorOf(Props[StaticFileProcessor]
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "my-router")

    // Start web server
    webServer = new WebServer(WebServerConfig(port = port), routes)
    webServer.start()

    // Wait for start
    Thread.sleep(1000)
  }

  override def afterAll(configMap: Map[String, Any]) {
    webServer.stop()

    if (router != null) {
      actorSystem.stop(router)
      router = null
    }

    if (tempDir != null) {
      deleteDirectory(tempDir)
      tempDir = null
    }
    if (rootDir != null) {
      deleteDirectory(rootDir)
      rootDir = null
    }

    actorSystem.shutdown()
    actorSystem = null
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

  def writeTextFile(path: File, content: String) {
    val out = new PrintStream(path, "UTF-8")
    out.print(content)
    out.flush()
    out.close()
  }

  "StaticFileProcessor" should {

    "correctly HTTP GET a file" in {
      val content = "test data test data test data"
      val file = new File(rootDir, "gettest1.txt")
      writeTextFile(file, content)

      val url = new URL(path + "files/gettest1.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      val resp = getResponseContent(conn)
      log.debug(resp.toString)

      resp.status should equal("200")
      resp.content should equal(content)
      resp.headers("Date").length should be > 0
      resp.headers("Content-Type") should equal("text/plain")
      resp.headers("Cache-Control") should equal("private, max-age=60")

      val fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
      fmt.setTimeZone(TimeZone.getTimeZone("GMT"))
      resp.headers("Last-Modified") should equal(fmt.format(new Date(file.lastModified())))

      val x = resp.headers("Date")
      val date = fmt.parse(resp.headers("Date"))
      val expires = fmt.parse(resp.headers("Expires"))
      (expires.getTime - date.getTime) should equal(browserCacheTimeoutSeconds * 1000)
    }

    "correctly cache a file" in {
      val sb = new StringBuilder
      for (i <- 1 to 100000) sb.append("a")
      val content = sb.toString

      val file = new File(rootDir, "getCached.txt")
      writeTextFile(file, content)

      // Initial get
      val url = new URL(path + "files/getCached.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.content should equal(content)
      resp.headers("Date").length should be > 0
      resp.headers("Content-Type") should equal("text/plain")
      resp.headers("Cache-Control") should equal("private, max-age=60")
      val lastModified = resp.headers("Last-Modified")

      // Getting file again should get a 304 Not Modified
      val conn2 = url.openConnection().asInstanceOf[HttpURLConnection];
      conn2.setRequestProperty("If-Modified-Since", lastModified)
      val resp2 = getResponseContent(conn2)
      log.debug(resp2.toString)

      resp2.status should equal("304")
      resp2.headers("Date").length should be > 0

      // Update file to force new file timestamp
      // Last modified should be cached
      val content3 = "test"
      writeTextFile(file, content3)
      file.setLastModified(new Date().getTime + 2000)

      val conn3 = url.openConnection().asInstanceOf[HttpURLConnection];
      conn3.setRequestProperty("If-Modified-Since", lastModified)
      val resp3 = getResponseContent(conn3)
      log.debug(resp3.toString)

      resp3.status should equal("304")
      resp3.headers("Date").length should be > 0

      // Wait until cache times out
      // We should get the file again
      Thread.sleep(2000)

      val conn4 = url.openConnection().asInstanceOf[HttpURLConnection];
      conn4.setRequestProperty("If-Modified-Since", lastModified)
      val resp4 = getResponseContent(conn4)
      log.debug(resp4.toString)

      resp4.status should equal("200")
      resp4.content should equal("test")
      resp4.headers("Date").length should be > 0
      resp4.headers("Content-Type") should equal("text/plain")
      resp4.headers("Cache-Control") should equal("private, max-age=60")
      resp4.headers("Last-Modified") should not equal (lastModified)
    }

    "return '404 Not Found' if requested file is outside specified root directory" in {
      val url = new URL(path + "files/../file.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      val resp = getResponseContent(conn)
      log.debug(resp.toString)

      resp.status should equal("404")
    }

    "return '404 Not Found' if requested file does not exist inside root directory" in {
      val url = new URL(path + "files/notexist.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      val resp = getResponseContent(conn)
      log.debug(resp.toString)

      resp.status should equal("404")
      resp.headers("Date").length should be > 0
    }

    "return '404 Not Found' if requested file is hidden" in {
      val content = "test data test data test data"
      val file = new File(rootDir, ".hidden.txt")
      writeTextFile(file, content)

      val url = new URL(path + "files/.hidden.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      val resp = getResponseContent(conn)
      log.debug(resp.toString)

      resp.status should equal("404")
      resp.headers("Date").length should be > 0
    }

    "return '404 Not Found' if requested file is a directory" in {
      val file = new File(rootDir, "directory.txt")
      file.mkdir()

      val url = new URL(path + "files/directory.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      val resp = getResponseContent(conn)
      log.debug(resp.toString)

      resp.status should equal("404")
      resp.headers("Date").length should be > 0
    }

    "correctly return GZIP encode content" in {
      val sb = new StringBuilder
      for (i <- 1 to 100000) sb.append("b")
      val content = sb.toString

      val file = new File(rootDir, "getGZippedContent.txt")
      writeTextFile(file, content)

      // Initial get
      val url = new URL(path + "files/getGZippedContent.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      conn.setRequestProperty("Accept-Encoding", "gzip")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.headers("Date").length should be > 0
      resp.headers("Content-Encoding") should equal("gzip")
      resp.content should equal(content)
      val lastModified = resp.headers("Last-Modified")
      log.debug(resp.headers.toString)

      // Getting file again should get a 304 Not Modified
      val conn2 = url.openConnection().asInstanceOf[HttpURLConnection];
      conn2.setRequestProperty("If-Modified-Since", lastModified)
      val resp2 = getResponseContent(conn2)
      log.debug(resp2.toString)

      resp2.status should equal("304")
      resp2.headers("Date").length should be > 0
    }

    "correctly return DEFLATE encode content" in {
      val sb = new StringBuilder
      for (i <- 1 to 100000) sb.append("b")
      val content = sb.toString

      val file = new File(rootDir, "getDeflatedContent.txt")
      writeTextFile(file, content)

      // Initial get
      val url = new URL(path + "files/getDeflatedContent.txt")
      val conn = url.openConnection().asInstanceOf[HttpURLConnection];
      conn.setRequestProperty("Accept-Encoding", "deflate")
      val resp = getResponseContent(conn)

      resp.status should equal("200")
      resp.headers("Date").length should be > 0
      resp.headers("Content-Encoding") should equal("deflate")
      resp.content should equal(content)
      val lastModified = resp.headers("Last-Modified")
      log.debug(resp.headers.toString)

      // Getting file again should get a 304 Not Modified
      val conn2 = url.openConnection().asInstanceOf[HttpURLConnection];
      conn2.setRequestProperty("If-Modified-Since", lastModified)
      val resp2 = getResponseContent(conn2)
      log.debug(resp2.toString)

      resp2.status should equal("304")
      resp2.headers("Date").length should be > 0
    }

    "be able to handle a little stress" in {
      val content = "test data test data test data"
      val file = new File(rootDir, "getStressed.txt")
      writeTextFile(file, content)

      // Start 10 threads - each thread will do 30 GETs
      val startTime = new Date().getTime
      val url = path + "files/getStressed.txt"
      val threads = new collection.mutable.ListBuffer[GetStaticFileThread]
      for (i <- 1 to 10) {
        val t = new GetStaticFileThread("Client_" + i, url, 30, content)
        t.start
        threads += t
      }

      // Wait for threads to finish
      var isAnyAlive = false
      threads.foreach(t => if (t.isAlive) isAnyAlive = true)
      while (isAnyAlive) {
        Thread.sleep(1000)
        isAnyAlive = false
        threads.foreach(t => if (t.isAlive) isAnyAlive = true)
      }

      // Finish
      val endTime = new Date().getTime - startTime
      log.info("Duration: " + endTime + " milliseconds")
      threads.foreach(t => t.hasErrors should be(false))
    }

  }
}

/**
 * Makes a HTTP GET request for a static file in a separate thread
 *
 * @param name Name to print in log
 * @param url URL to GET
 * @param count Number of HTTP requests to make
 * @param content Expected content of HTTP get
 */
class GetStaticFileThread(name: String, url: String, count: Int, content: String)
  extends Thread with TestHttpClient with Logger {

  var hasErrors = false
  override def run(): Unit = {
    for (i <- 1 to count) {

      // Add query string to provide identity of thread and connection count
      // Format is: Client_[Thread #]_[connection #]
      val id = name + "_" + i
      val u = new URL(url + "?id=" + id)
      val conn = u.openConnection().asInstanceOf[HttpURLConnection]
      val resp = getResponseContent(conn)
      conn.disconnect()

      if (resp.status != "200") {
        log.error("Status Error in {}: {} ", Array(id, resp.status))
        hasErrors = true
      }
      if (resp.content != content) {
        log.error("Comparison Error in {}: {} {}", Array(id, content, resp.content))
        hasErrors = true
      }

      // Give it a rest otherwise we get connections not being reused correctly
      Thread.sleep(30)
    }
  }
}