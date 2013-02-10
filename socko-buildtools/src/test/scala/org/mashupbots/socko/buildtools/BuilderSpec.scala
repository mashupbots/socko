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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.mashupbots.socko.infrastructure.IOUtil
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.WatchEvent
import org.mashupbots.socko.infrastructure.Logger
import java.nio.file.Files
import org.mashupbots.socko.infrastructure.CharsetUtil

class BuilderSpec extends WordSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterAll with Logger {

  "Builder should watch and build" in {
    val root = IOUtil.createTempDir("BuilderSpec")
    
    val src = new File(root, "src")
    src.mkdir()
    IOUtil.writeTextFile(new File(src, "one.js"), "1111")
    IOUtil.writeTextFile(new File(src, "two.js"), "2222")
    
    val target = new File(root, "target")
    target.mkdir()
    
    val buildFile = new File(root, "build.xml")
    val buildFileContents = """
      <project name="MyProject" default="concat" basedir=".">
    	<!-- set global properties for this build -->
    	<property name="src" location="src"/>
		<property name="target"  location="target"/>
        
        <target name="concat" description="concat our files" >
          <concat destfile="${target}/t1.js">
            <filelist dir="${src}">
	          <file name="one.js" />
	          <file name="two.js" />
    		</filelist>
		  </concat>
    	</target>
      
      </project>
      """
    IOUtil.writeTextFile(buildFile, buildFileContents)

    // Start
    val builder = new Builder(s"internal-ant ${root.toString}/build.xml", s"${root.toString}/src")
    Thread.sleep(500)

    // See if build run on start up
    val t1 = new File(target, "t1.js")
    t1.exists should be (true)
    Files.readAllLines(FileSystems.getDefault().getPath(t1.getPath()), CharsetUtil.UTF_8).get(0) should be ("11112222")

    // Change src file
    IOUtil.writeTextFile(new File(src, "one.js"), "one")
    Thread.sleep(500)
    
    // See if new file
    val t11 = new File(target, "t1.js")
    t11.exists should be (true)
    Files.readAllLines(FileSystems.getDefault().getPath(t11.getPath()), CharsetUtil.UTF_8).get(0) should be ("one2222")

    // Stop
    builder.stop()
    Thread.sleep(1100)
    builder.isAlive() should be (false)
    
    // Finish
    IOUtil.deleteDir(root)
  }

}