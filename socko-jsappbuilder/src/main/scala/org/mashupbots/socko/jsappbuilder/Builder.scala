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
package org.mashupbots.socko.jsappbuilder

import java.io.File

import org.mashupbots.socko.infrastructure.IOUtil

import akka.actor.Actor
import akka.actor.ActorRef

/**
 * Boss actor for controlling task execution
 *
 * @param rootDirectory Root directory to use for relative paths specified in the configuration
 * @param config Build configuration
 * @param listener Actor responsible for displaying messages to the user
 */
class Builder(
  rootDirectory: File,
  config: JsAppBuilderConfig,
  listener: ActorRef) extends Actor {

  checkDirectory(rootDirectory, "Root")
  config.validate()

  /**
   * Root source directory where source files are kept
   */
  val rootSourceDirectory =
    if (IOUtil.isAbsolutePath(config.rootSourceDirectory)) new File(config.rootSourceDirectory)
    else new File(rootDirectory, config.rootSourceDirectory)
  checkDirectory(rootSourceDirectory, "Source")

  /**
   * Root target directory where output files are kept. When cleaning, this directory is deleted.
   */
  val rootTargetDirectory =
    if (IOUtil.isAbsolutePath(config.rootTargetDirectory)) new File(config.rootTargetDirectory)
    else new File(rootDirectory, config.rootTargetDirectory)
  checkDirectory(rootTargetDirectory, "Target")

  /**
   * Root target directory where output files are kept. When cleaning, this directory is deleted.
   */
  val rootTempDirectory =
    if (config.rootTempDirectory.isEmpty) IOUtil.createTempDir("jsappbuilder_" + System.currentTimeMillis())
    else if (IOUtil.isAbsolutePath(config.rootTempDirectory.get)) new File(config.rootTempDirectory.get)
    else new File(rootDirectory, config.rootTempDirectory.get)
  checkDirectory(rootTempDirectory, "Temp")

  /**
   * Checks if the specified file is a directory and exists
   */
  private def checkDirectory(dir: File, name: String) {
    if (!dir.exists) {
      throw new IllegalArgumentException("%s directory '%s' does not exist".format(name, dir.getPath))
    }
    if (!dir.isDirectory) {
      throw new IllegalArgumentException("%s directory '%s' is a file".format(name, dir.getPath))
    }
  }

  /**
   * Process messages
   */
  def receive = {
    case CleanRequest => {
      try {
        listener ! InfoStatusReport("Cleaning ...")
        
        listener ! VerboseStatusReport("Deleting " + rootTargetDirectory.getAbsolutePath)
        IOUtil.deleteDir(rootTargetDirectory)
        
        listener ! VerboseStatusReport("Deleting " + rootTempDirectory.getAbsolutePath)
        IOUtil.deleteDir(rootTempDirectory)
        
        listener ! CleanResponse(None)
      } catch {
        case ex: Exception => listener ! CleanResponse(Some(ex))
      }
    }

    case BuildRequest => {

    }

    case WatchRequest => {

    }
  }
}

/**
 * Base trait common to all builder messages
 */
sealed trait BuilderMessage

/**
 * Base trait common to all builder responses
 */
sealed trait BuilderRequest extends BuilderMessage

/**
 * Base trait common to all builder responses
 */
sealed trait BuilderResponse extends BuilderMessage {
  val error: Option[Exception]
  def isSuccess = error.isEmpty
}

/**
 * Request to clean the target directory
 */
case class CleanRequest() extends BuilderRequest

/**
 * Response to report on the status of cleaning
 */
case class CleanResponse(error: Option[Exception]) extends BuilderResponse

/**
 * Request to execute a build
 */
case class BuildRequest() extends BuilderRequest

/**
 * Response to build execution
 */
case class BuildResponse(error: Option[Exception]) extends BuilderResponse

/**
 * Request to watch all source files and trigger a build a task if source files for that task have
 * been modified.
 */
case class WatchRequest() extends BuilderRequest

/**
 * Response to watch execution
 */
case class WatchResponse(error: Option[Exception]) extends BuilderResponse

/**
 * Report on what is currently happening
 */
sealed trait StatusReport extends BuilderMessage {
  def message: String
}
case class VerboseStatusReport(message: String) extends StatusReport
case class InfoStatusReport(message: String) extends StatusReport
case class ErrorStatusReport(message: String) extends StatusReport
