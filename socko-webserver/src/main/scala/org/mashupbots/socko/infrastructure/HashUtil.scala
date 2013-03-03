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

import java.security.MessageDigest

object HashUtil {

  /**
   * Calculate an MD5 has of a string. Used to hashing a file name
   *
   * @param s String to MD5 hash
   * @return MD5 hash of specified string
   */
  def md5(s: String): String = {
    md5(s.getBytes)
  }

  /**
   * Calculate an MD5 has of a string. Used to hashing a file name
   *
   * Code thanks to [[http://code-redefined.blogspot.com.au/2009/05/md5-sum-in-scala.html Code Redefined]]
   *
   * @param bytes Data to hash
   * @return MD5 hash of specified string
   */
  def md5(bytes: Array[Byte]): String = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(bytes)
    md5.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }

}