//
// Copyright 2013 Vibul Imtarnasan, David Bolton and Socko contributors.
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
package org.mashupbots.socko.buildtools

import java.io.File
import java.nio.file.WatchEvent

import scala.sys.process.Process

import org.apache.tools.ant.DefaultLogger
import org.apache.tools.ant.Project
import org.apache.tools.ant.ProjectHelper
import org.mashupbots.socko.infrastructure.Logger

/**
 * Responsible for running the build file
 *
 * @param commandLine Command line to call to build. The build is run in another process.
 *
 *   There is a special command line to run ant internally within this VM rather than an external 
 *   process in a new VM.
 *
 *   {{{
 *   internal-ant [build-file] [target1] [target2] [target3] ...
 *   }}}
 *
 *   This may execute your ant scripts faster because a new process does not have to be created.
 *   
 *   However, there are limitations:
 *     - `build-file` must be the absolute path to the build file 
 *     - `build-file` and target names cannot have spaces in them.
 *     -  No other ant options are supported. If you wish to use advance ant options, just use the normal ant.
 * 
 */
case class BuildRunner(val commandLine: String) extends Logger {

  val isInternalAntBuild = commandLine.startsWith("internal-ant")
  val internalAntParams: List[String] = if (isInternalAntBuild) commandLine.replaceAll("[\\s]+", " ").split(" ").toList else Nil

  /**
   * Run build process
   *
   * See this [[http://stackoverflow.com/questions/6013415/how-does-the-scala-sys-process-from-scala-2-9-work atricle]]
   * for more info.
   *
   * @param events List of events passed in by [[org.mashupbots.socko.buildtools.DirectoryWatcher]]
   */
  def runBuild(events: List[WatchEvent[_]] = Nil) {
    try {
      if (commandLine != "") {
        log.info("Build Started")

        if (isInternalAntBuild) {
          runInternalAntBuild()
        } else {
          val pb = Process(commandLine)
          val exitCode = pb.!
        }

        log.info("Build Finished")
      }
    } catch {
      case ex: Exception => {
        log.error("Build Error: " + ex.getMessage(), ex)
        sys.exit(1) // Exit if there is a build error
      }
    }
  }

  /**
   * Runs an ant build internally. Maybe a little faster than forking a new process.
   *
   * See:
   * http://www.ibm.com/developerworks/websphere/library/techarticles/0502_gawor/0502_gawor.html
   * http://stackoverflow.com/questions/3684279/invoke-ant-programmatically-using-java-with-parameter
   * http://stackoverflow.com/questions/6386349/call-ant-build-through-java-code
   * http://thilosdevblog.wordpress.com/2010/08/30/calling-an-ant-target-via-java/
   */
  def runInternalAntBuild() {
    val buildFile = new File(internalAntParams(1))

    val p = new Project()
    p.setUserProperty("ant.file", buildFile.getAbsolutePath());

    val consoleLogger = new DefaultLogger()
    consoleLogger.setErrorPrintStream(System.err)
    consoleLogger.setOutputPrintStream(System.out)
    consoleLogger.setMessageOutputLevel(Project.MSG_INFO)
    p.addBuildListener(consoleLogger)

    p.fireBuildStarted()
    p.init()

    val helper = ProjectHelper.getProjectHelper()
    p.addReference("ant.projectHelper", helper)
    helper.parse(p, buildFile)

    val targets = {
      val t = internalAntParams.drop(2)
      if (t.isEmpty) {
        p.getDefaultTarget() :: Nil
      } else {
        t
      }
    }
    targets.foreach(t => p.executeTarget(t))

    p.fireBuildFinished(null)
  }

}