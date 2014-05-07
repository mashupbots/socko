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

import org.mashupbots.socko.infrastructure.IOUtil
import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.Matchers
import org.scalatest.WordSpec

class BuildRunnerSpec extends WordSpec with Matchers with Logger {

  "Internal ant build should work" in {
    val root = IOUtil.createTempDir("BuildRunnerSpec")
    
    val src = new File(root, "src")
    src.mkdir()
    IOUtil.writeTextFile(new File(src, "one.js"), "1111")
    IOUtil.writeTextFile(new File(src, "two.js"), "2222")
    IOUtil.writeTextFile(new File(src, "three.js"), "3333")
    IOUtil.writeTextFile(new File(src, "four.js"), "4444")
    
    val target = new File(root, "target")
    target.mkdir()
    
    val buildFile = new File(root, "build.xml")
    val buildFileContents = """
      <project name="MyProject" basedir=".">
    	<!-- set global properties for this build -->
    	<property name="src" location="src"/>
		<property name="target"  location="target"/>
  
		<target name="init">
	      <tstamp/>
	    </target>
      
        <target name="t1" depends="init" description="work our js file" >
          <concat destfile="${target}/t1.js">
            <filelist dir="${src}">
	          <file name="one.js" />
	          <file name="two.js" />
    		</filelist>
		  </concat>
    	</target>

        <target name="t2" depends="init" description="work our js file" >
          <concat destfile="${target}/t2.js">
            <filelist dir="${src}">
	          <file name="three.js" />
	          <file name="four.js" />
    		</filelist>
		  </concat>
    	</target>
      
      </project>
      """
    IOUtil.writeTextFile(buildFile, buildFileContents)

    val buildRunner = new BuildRunner(s"internal-ant ${root.toString}/build.xml t1 t2")
    buildRunner.runInternalAntBuild()

    val t1 = new File(target, "t1.js")
    t1.exists should be (true)

    val t2 = new File(target, "t2.js")
    t2.exists should be (true)
    
    IOUtil.deleteDir(root)
  }

}