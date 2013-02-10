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
package org.mashupbots.socko.examples.builder

import java.io.File
import java.io.FileOutputStream
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers.StaticContentHandler
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.handlers.StaticFileRequest
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.IOUtil
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig
import org.mashupbots.socko.handlers.StaticContentHandlerConfig
import org.mashupbots.socko.buildtools.Builder

/**
 * Demonstrates how to use the [[org.mashupbots.socko.buildtools.Builder]] to automatically
 * run your build when a source file is changed.
 */
object BuilderApp extends Logger {

  val targetDir = createTempDir("target_")
  val srcDir = createTempDir("src_")
  
  // Important to make sure you turn off caching so you can see your changes when 
  // you refresh your browser
  val staticContentHandlerConfig = StaticContentHandlerConfig(
    rootFilePaths = Seq(targetDir.getAbsolutePath),
    serverCacheMaxFileSize = 0,
    serverCacheTimeoutSeconds = 0,
    browserCacheTimeoutSeconds = 0)

  //
  // STEP #1 - Define Actors and Start Akka
  //
  val actorConfig = """
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

  val actorSystem = ActorSystem("BuilderActorSystem", ConfigFactory.parseString(actorConfig))
  val staticContentHandlerRouter = actorSystem.actorOf(Props(new StaticContentHandler(staticContentHandlerConfig))
    .withRouter(FromConfig()).withDispatcher("my-dispatcher"), "static-file-router")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case HttpRequest(request) => request match {
      case GET(Path("/index.html")) => {
        staticContentHandlerRouter ! new StaticFileRequest(request, new File(targetDir, "index.html"))
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
    // Create source
    val headerFile = new File(srcDir, "header.html")
    IOUtil.writeTextFile(headerFile, "<html><body><h1>Builder Test Page</h1>")
    
    val contentFile = new File(srcDir, "content.html")
    IOUtil.writeTextFile(contentFile, "Hello there.")

    val footerFile = new File(srcDir, "footer.html")
    IOUtil.writeTextFile(footerFile, "</body></html>")

    // Create build file
    val buildFile = new File(srcDir, "build.xml")
    val buildFileContents = s"""
      <project name="MyProject" default="concat" basedir=".">
    	<!-- set global properties for this build -->
    	<property name="src" location="."/>
		<property name="target" location="${targetDir.getCanonicalPath()}"/>
        
        <target name="concat" description="concat our files" >
          <concat destfile="$${target}/index.html">
            <filelist dir="$${src}">
	          <file name="header.html" />
	          <file name="content.html" />
	          <file name="footer.html" />
    		</filelist>
		  </concat>
    	</target>
      
      </project>
      """
    IOUtil.writeTextFile(buildFile, buildFileContents)
    
    // Start
    // Note that we are using the internal ant build tool. You can use your own by specifying your command line
    val builder = new Builder(s"internal-ant ${srcDir.toString}/build.xml", s"${srcDir.toString}")
    
    // Start web server to serve files from the target dir 
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run {
        webServer.stop()
        targetDir.delete()
        srcDir.delete()
      }
    })
    webServer.start()

    System.out.println("")
    System.out.println("")
    System.out.println("Open your browser and navigate to http://localhost:8888/index.html")
    System.out.println("Source directory is " + srcDir.getAbsolutePath)
    System.out.println("Target directory is " + targetDir.getAbsolutePath)
    System.out.println("")
    System.out.println("When you change a file in the source directory, the build is run.")
    System.out.println("Refresh your browser to see your changes.")
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

}
