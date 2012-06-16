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

    var ret = Array[Byte]()
    var buffer = new Array[Byte](4096)
    while (is.available > 0) { // "available" is not always the exact size
      val bytesRead = is.read(buffer)
      ret = ret ++ buffer.take(bytesRead)
    }
    is.close()
    ret
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
   * Auto close streams with [[http://stackoverflow.com/questions/2207425/what-automatic-resource-management-alternatives-exists-for-scala automatic resource management]]
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
  def using[T <: { def close() }](resource: T)(block: T => Unit) {
    try {
      block(resource)
    } finally {
      if (resource != null) resource.close()
    }
  }

}