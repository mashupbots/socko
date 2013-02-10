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

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable
import org.mashupbots.socko.infrastructure.Logger

import language.implicitConversions

/**
 * Watches a directory for changes and triggers a build
 *
 * Thanks to [[https://gist.github.com/1241375 Chris Eberle]].
 * Also see [[http://docs.oracle.com/javase/tutorial/essential/io/notification.html java tutorial]]
 *
 * @param path Path to directory to watch
 * @param recursive Watch sub directories too?
 * @param onEvent Function to call when something changed on the file system
 * @param onPoll Optional function to call after every `eventPollTimeout` and there are no events to process. If 
 *   the function returns `true`, polling continues. If `false` is returned, polling is terminated and the thread 
 *   is stopped.
 * @param eventDelayTimeout Milliseconds to wait after an event for more events. `onEvent` will only be called
 *   after this delay has expired and there are no more events.
 * @param eventPollTimeout Milliseconds to wait for an event. If no events have been raised, `onPoll()` is called if
 *   it has been supplied.
 */
class DirectoryWatcher(
  val path: Path,
  val recursive: Boolean,
  val onEvent: (List[WatchEvent[_]]) => Unit,
  val onPoll: Option[() => Boolean] = None,
  val eventDelayTimeout: Int = 100,
  val eventPollTimeout: Int = 10000) extends Runnable with Logger {

  val watchService = path.getFileSystem().newWatchService()
  val keys = new HashMap[WatchKey, Path]

  /**
   * Register a particular file or directory to be watched
   */
  def register(dir: Path): Unit = {
    val key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_MODIFY,
      StandardWatchEventKinds.ENTRY_DELETE)

    // Debug info
    val prev = keys.getOrElse(key, null)
    if (prev == null) {
      log.debug("Registering: " + dir)
    } else {
      if (!dir.equals(prev)) {
        log.debug("Update: " + prev + " -> " + dir)
      }
    }

    keys(key) = dir
  }

  /**
   * Makes it easier to walk a file tree
   */
  implicit def makeDirVisitor(f: (Path) => Unit) = new SimpleFileVisitor[Path] {
    override def preVisitDirectory(p: Path, attrs: BasicFileAttributes) = {
      f(p)
      FileVisitResult.CONTINUE
    }
  }

  /**
   *  Recursively register directories
   */
  def registerAll(start: Path): Unit = {
    Files.walkFileTree(start, (f: Path) => {
      register(f)
    })
  }

  /**
   * Run a build
   */
  def logEvent(event: WatchEvent[_]): Unit = {
    val kind = event.kind
    val event_path = event.context().asInstanceOf[Path]
    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
      log.debug("Entry created: {}", event_path)
    } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
      log.debug("Entry deleted: {}", event_path)
    } else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
      log.debug("Entry modified: {}", event_path)
    }
  }

  /**
   * The main directory watching thread
   */
  override def run(): Unit = {
    try {
      if (recursive) {
        log.info("Scanning '{}' for files to watch", path)
        registerAll(path)
        log.info("Finished Scanning")
      } else {
        register(path)
      }

      val buffer = new ListBuffer[WatchEvent[_]]()
      breakable {
        while (true) {
          // If build required, then let's have a short delay; otherwise have a longer
          // 10 second delay so we use less resources
          val timeout = if (buffer.isEmpty) eventPollTimeout else eventDelayTimeout
          log.debug("Polling for {}", timeout)

          // Wait for event
          val key = watchService.poll(timeout, TimeUnit.MILLISECONDS)
          if (key == null) {
            // Build once per set of raised events. Wait until no events for X milliseconds
            // before doing a build.

            // For example, saving 1 file may have several events depending on the editor and
            // we don't want to do a build for each event!
            //   - Entry created: .goutputstream-DNG2JW
            //   - Entry modified: .goutputstream-DNG2JW
            //   - Entry deleted: .goutputstream-DNG2JW
            //   - Entry created: one.js
            if (buffer.isEmpty) {
              if (onPoll.isDefined) 
                if (!onPoll.get())
                  break
            } else {
              onEvent(buffer.toList)
              buffer.clear()
            }
          } else {
            // Process keys
            val dir = keys.getOrElse(key, null)
            if (dir != null) {
              // Process raised events 
              key.pollEvents().asScala.foreach(event => {
                // Add to buffer for passing to onEvent handler
                buffer.append(event)

                val kind = event.kind
                if (kind != StandardWatchEventKinds.OVERFLOW) {
                  val name = event.context().asInstanceOf[Path]
                  val child = dir.resolve(name)

                  logEvent(event)

                  if (recursive && (kind == StandardWatchEventKinds.ENTRY_CREATE)) {
                    try {
                      if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        registerAll(child);
                      }
                    } catch {
                      case ioe: IOException => println("IOException: " + ioe)
                      case e: Exception =>
                        log.error("Error watching new directory " + e.getMessage, e)
                        break
                    }
                  }
                }
              })
            } else {
              log.error("WatchKey not recognized!!")
            }

            if (!key.reset()) {
              keys.remove(key);
              if (keys.isEmpty) {
                break
              }
            }

          }
        }	// while
      }		// breakable
    } catch {
      case e: Exception => log.error("Error watching file: " + e.getMessage, e)
    }
  }
}
