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
package org.mashupbots.socko.buildtools;

import java.nio.file.FileSystems
import org.mashupbots.socko.infrastructure.Logger

/**
 * Watches a directory and if there is a change, the build is run.
 *
 * A new thread is created for watching and building.
 *
 * ==Usage==
 * {{{
 * // Start watching and building
 * val builder = new Builder("internal-ant /my-project/build.xml build", "/my-project/src")
 * 
 * ...
 * 
 * // Stop
 * builder.stop()
 * }}}
 *
 *
 * @commandLine Command line to run when a watched file changes.
 *   For example
 *   {{{
 *   ant -f /home/username/dev/project/build.xml js
 *   }}}
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
 * @directoryToWatch Full path to the directory to watch
 * @watchRecursively Indicates if we wish to watch files in sub-directories as well
 * @param eventDelayTimeout Milliseconds to wait after an event for more events before a build is triggered.
 *   If we do not wait, multiple builds may be kicked off when saving a single file. Defaults to `100`.
 */
case class Builder(commandLine: String,
  directoryToWatch: String,
  watchRecursively: Boolean = true,
  eventDelayTimeout: Int = 100) extends Logger {

  private var isStopped = false

  // Build
  private val buildRunner = BuildRunner(commandLine)
  buildRunner.runBuild()

  // Watch
  private val path = FileSystems.getDefault().getPath(directoryToWatch)
  private val dirWatcher = new DirectoryWatcher(path, true, buildRunner.runBuild, Some(this.onPoll), eventDelayTimeout, 1000)
  private val watchThread = new Thread(dirWatcher)
  watchThread.start()

  /**
   * Indicates if thread is still running
   * @returns True if thread is still, false if not
   */
  def isAlive(): Boolean = watchThread.isAlive()

  /**
   * Passed into `DirectoryWatcher` to let it know if we want to stop or not
   * @returns True if we want to continue, false if we want to stop
   */
  private def onPoll(): Boolean = {
    !isStopped
  }

  /**
   * Stop watching and building. 
   * 
   * The thread will be terminated by exiting of the run method.
   */
  def stop(): Unit = {
    isStopped = true
  }

}
