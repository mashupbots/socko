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

import javax.activation.MimetypesFileTypeMap
import java.io.File

/**
 * MIME types lookup
 *
 * This implementation uses <a href="http://docs.oracle.com/javase/6/docs/api/javax/activation/MimetypesFileTypeMap.html">
 * `MimetypesFileTypeMap`</a> and relies on the presence of the file extension in a `mime.types` file.
 *
 * See
 *  - https://github.com/klacke/yaws/blob/master/src/mime.types
 *  - http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types
 *  - http://download.oracle.com/javaee/5/api/javax/activation/MimetypesFileTypeMap.html
 *  - src/main/resources/META-INF/mime.types
 */
object MimeTypes {
  private[this] val map = new MimetypesFileTypeMap

  /**
   * Returns the MIME type from a file name.
   *
   * If no MIME type is found, `application/octet-stream` is returned.
   *
   * @param fileName name of file
   */
  def get(fileName: String): String = {
    map.getContentType(fileName)
  }

  /**
   * Returns the MIME type from a file
   *
   * If no MIME type is found, `application/octet-stream` is returned.
   *
   * @param file File
   */
  def get(file: File): String = {
    map.getContentType(file)
  }

}