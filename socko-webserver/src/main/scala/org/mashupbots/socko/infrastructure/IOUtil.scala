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
package org.mashupbots.socko.infrastructure

import java.io.InputStream
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import scala.language.reflectiveCalls
import scala.util.matching.Regex

/**
 * Utility IO methods
 */
object IOUtil {

  /**
   * Read bytes from an input stream. The input stream will be closed by this method after reading
   *
   * @param is Input stream
   */
  def readInputStream(is: InputStream): Array[Byte] = {
    if (is == null) return null

    val baos = new ByteArrayOutputStream()
    try {
      val buffer = new Array[Byte](4096)
      while (is.available > 0) { // "available" is not always the exact size
        val bytesRead = is.read(buffer)
        baos.write(buffer, 0, bytesRead)
      }
      is.close()
      baos.toByteArray()
    } finally {
      baos.close()
    }
  }

  /**
   * Pipe input to output stream using 8K blocks
   *
   * @param bytesIn Input stream
   * @param bytesOut Output stream
   */
  def pipe(bytesIn: InputStream, bytesOut: OutputStream): Unit = {
    val buf = new Array[Byte](8192)
    def doPipe(): Unit = {
      val len = bytesIn.read(buf)
      if (len > 0) {
        bytesOut.write(buf, 0, len)
        doPipe()
      }
    }
    doPipe()
  }

  /**
   * Returns the contents of a file as bytes. Throws `FileNotFoundException` if the file is not found.
   *
   * @param filePath Full path to file to read
   */
  def readFile(filePath: String): Array[Byte] = {
    val is = new BufferedInputStream(new FileInputStream(filePath))
    readInputStream(is)
  }

  /**
   * Returns the contents of a file as bytes.  Throws `FileNotFoundException` if the file is not found.
   *
   * @param file File to read
   */
  def readFile(file: File): Array[Byte] = {
    readFile(file.getAbsolutePath)
  }

  /**
   * Returns the contents of a resource as bytes; `null` if the resource was not found.
   *
   * @param classpath Class path of the resource in the classpath.  Do **NOT** put a leading "/".
   *   See [[http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html?page=2 article]]
   */
  def readResource(classpath: String): Array[Byte] = {
    val is = getClass.getClassLoader.getResourceAsStream(classpath)
    readInputStream(is)
  }

  /**
   * Write a text file
   *
   * @param file File to write to. If the file exists, it will be overwritten
   * @param text Text to write to the file
   * @param charset Character set. Defaults to UTF-8.
   * @return file
   */
  def writeTextFile(file: File, text: String, charset: Charset = CharsetUtil.UTF_8): File = {
    using(new FileOutputStream(file)) { out =>
      out.write(text.getBytes(charset))
    }
    file
  }

  /**
   * Auto close streams with [[http://stackoverflow.com/questions/2207425/what-automatic-resource-management-alternatives-exists-for-scala automatic resource management]]
   *
   * Also see Programming in Scala Second Edition Chapter 20.8.
   *
   * Example usage
   * {{{
   * using(new BufferedReader(new FileReader("file"))) { r =>
   *   var count = 0
   *   while (r.readLine != null) count += 1
   *   println(count)
   * }
   * }}}
   */
  def using[T <: { def close() }, A](resource: T)(block: T => A) = {
    try {
      block(resource)
    } finally {
      if (resource != null) resource.close()
    }
  }

  /**
   * Returns a newly created temporary directory in the system defined temporary directory
   *
   * @param namePrefix Prefix to use on the directory name
   * @return Newly created directory
   */
  def createTempDir(namePrefix: String): File = {
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
  def deleteDir(dir: File) {
    if (dir.exists) {
      val files = dir.listFiles()
      files.foreach(f => {
        if (f.isFile) {
          f.delete()
        } else {
          deleteDir(f)
        }
      })
    }
    dir.delete()
  }

  /**
   * Delete all the contents (files and sub-directories) of the specified directory
   *
   * @param dir Directory
   */
  def deleteDirContents(dir: File) {
    if (dir.exists && dir.isDirectory) {
      val files = dir.listFiles()
      files.foreach(f => {
        if (f.isFile) {
          f.delete()
        } else {
          deleteDir(f)
        }
      })
    }
  }

  /**
   * Tests if a path string is an absolute path or not
   *
   * Assumes that an absolute path starts with the file separator of if on windows, it starts
   * with a drive letter.
   */
  def isAbsolutePath(path: String): Boolean = {
    path.startsWith(File.separator) || path.matches("[A-Za-z]:\\\\.*")
  }

  /**
   * Returns a list of all files in a directory and its sub-directories
   *
   * @param f Directory to look under
   */
  def recursiveListFiles(f: File): Array[File] = {
    if (f == null) Array.empty
    else {
      val these = f.listFiles
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_))
    }
  }

  /**
   * Returns a list of all files in a directory and its sub-directories matching the specified regular expression
   *
   * @param f Directory to look under
   * @param r Regular expression to match file names
   * @see http://stackoverflow.com/questions/2637643/how-do-i-list-all-files-in-a-subdirectory-in-scala
   */
  def recursiveListFiles(f: File, r: Regex): Array[File] = {
    if (f == null) Array.empty
    else {
      val these = f.listFiles
      val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
      good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_, r))
    }
  }
}
