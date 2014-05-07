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
import java.nio.file.FileSystems
import java.nio.file.WatchEvent

import org.mashupbots.socko.infrastructure.IOUtil
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.Matchers
import org.scalatest.WordSpec

class DirectoryWatcherSpec extends WordSpec with Matchers with Logger {

  var triggerred = false
  var stop = false

  "DirectoryWatcher should raise events when file system changes" in {

    val root = IOUtil.createTempDir("DirectoryWatcherSpec")

    val path = FileSystems.getDefault().getPath(root.getAbsolutePath)
    val dirWatcher = new DirectoryWatcher(path, true, processEvents, Some(onPoll), 100, 500)
    val watchThread = new Thread(dirWatcher)
    watchThread.start()
    Thread.sleep(500)

    // Create file
    val testFile = new File(root, "test.txt")
    IOUtil.writeTextFile(testFile, "Test")
    Thread.sleep(500)
    triggerred should be(true)

    // Change file
    triggerred = false
    IOUtil.writeTextFile(testFile, "Test2")
    Thread.sleep(500)
    triggerred should be(true)

    // Delete file
    triggerred = false
    testFile.delete()
    Thread.sleep(500)
    triggerred should be(true)

    // Create directory
    triggerred = false
    val testDir = new File(root, "testdir")
    testDir.mkdir()
    Thread.sleep(500)
    triggerred should be(true)

    // Create file in new dir
    triggerred = false
    val testFile2 = new File(testDir, "test.txt")
    IOUtil.writeTextFile(testFile2, "Test")
    Thread.sleep(500)
    triggerred should be(true)
    
    // Delete file in new dir
    triggerred = false
    testFile2.delete()
    Thread.sleep(500)
    triggerred should be(true)

    // Delete directory
    triggerred = false
    testDir.delete()
    Thread.sleep(500)
    triggerred should be(true)

    IOUtil.deleteDir(root)
    
    stop = true
    Thread.sleep(1000)
    watchThread.isAlive() should be (false)
  }

  def processEvents(events: List[WatchEvent[_]] = Nil) {
    triggerred = true
  }

  def onPoll(): Boolean = {
    stop
  }
}