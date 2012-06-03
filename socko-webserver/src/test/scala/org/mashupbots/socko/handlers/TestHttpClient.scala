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
package org.mashupbots.socko.handlers

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLConnection
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import java.util.Hashtable

/**
 * Contains common methods used to call our web server
 */
trait TestHttpClient {

  val POST = "POST"
  val PUT = "PUT"
  val URLENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded"
  val BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy"

  /**
   * Send a put request
   *
   * @param conn HTTP Connection
   * @param contentType MIME type of content
   * @param charset Character set
   * @param content Text to send
   */
  def sendPutRequest(conn: HttpURLConnection, contentType: String, charset: Charset, content: String) {
    conn.setRequestMethod(PUT)
    conn.setRequestProperty("Content-Type", contentType)
    conn.setDoOutput(true)
    val os = conn.getOutputStream()
    os.write(content.getBytes(charset))
    os.flush()
  }

  /**
   * Send a post request
   *
   * @param conn HTTP Connection
   * @param contentType MIME type of content
   * @param charset Character set
   * @param content Text to send
   */
  def sendPostRequest(conn: HttpURLConnection, contentType: String, charset: Charset, content: String) {
    conn.setRequestMethod(POST)
    conn.setRequestProperty("Content-Type", contentType)
    conn.setDoOutput(true)
    val os = conn.getOutputStream()
    os.write(content.getBytes(charset))
    os.flush()
  }

  /**
   * Send a post with multipart file upload content
   * See http://www.developer.nokia.com/Community/Wiki/HTTP_Post_multipart_file_upload_in_Java_ME
   *
   * @param conn HTTP Connection
   * @param fileFieldName field name when file can be found
   * @param fileName the file's name on the client's hard disk
   * @param fileMimeType the file's content type
   * @param content file content
   */
  def sendPostFileUpload(conn: HttpURLConnection,
    params: Hashtable[String, String],
    fileFieldName: String,
    fileName: String,
    fileMimeType: String,
    content: Array[Byte]) {

    val boundaryMessage = getBoundaryMessage(BOUNDARY, params, fileFieldName, fileName, fileMimeType)

    val endBoundary = "\r\n--" + BOUNDARY + "--\r\n"

    val bos = new ByteArrayOutputStream()
    bos.write(boundaryMessage.getBytes)
    bos.write(content)
    bos.write(endBoundary.getBytes())
    val postBytes = bos.toByteArray()
    bos.close();

    conn.setRequestMethod(POST)
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
    conn.setDoOutput(true)
    val os = conn.getOutputStream()
    os.write(postBytes)
    os.flush()
  }

  private def getBoundaryMessage(boundary: String,
    params: Hashtable[String, String],
    fileField: String,
    fileName: String,
    fileType: String): String = {
    val res = new StringBuffer("--").append(boundary).append("\r\n")

    val keys = params.keys()
    while (keys.hasMoreElements()) {
      val key = keys.nextElement().asInstanceOf[String]
      val value = params.get(key).asInstanceOf[String]

      res.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n")
        .append("\r\n").append(value).append("\r\n")
        .append("--").append(boundary).append("\r\n")
    }
    res.append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(fileName).append("\"\r\n")
      .append("Content-Type: ").append(fileType).append("\r\n\r\n")

    res.toString()
  }

  /**
   * Get standard response form the supplied connection
   *
   * @param conn HTTP Connection
   */
  def getResponseContent(conn: HttpURLConnection): HttpResponseData = {
    try {
      val connIn = conn.getInputStream()
      val headers = collection.mutable.Map.empty[String, String]
      val statusLine = getResponseHeaders(conn, headers)

      val content = new StringBuilder
      if (connIn != null) {
        var in: BufferedReader = null
        val contentEncoding = headers.getOrElse("Content-Encoding", "")
        if (contentEncoding == "") {
          in = new BufferedReader(new InputStreamReader(connIn))
        } else {
          if (contentEncoding == "gzip") {
            in = new BufferedReader(new InputStreamReader(new GZIPInputStream(connIn)))
          } else if (contentEncoding == "deflate") {
            in = new BufferedReader(new InputStreamReader(new InflaterInputStream(connIn)))
          } else {
            throw new UnsupportedOperationException("Content encoding: " + contentEncoding)
          }
        }

        readAll(in, content)
        in.close()
      }

      HttpResponseData(statusLine, content.toString, headers)
    } catch {
      case ex =>
        System.out.println(ex.toString)
        getResponseErrorContent(conn)
    }
  }

  /**
   * Get error response in the event that response status is not 200
   *
   * @param conn HTTP Connection
   */
  def getResponseErrorContent(conn: HttpURLConnection): HttpResponseData = {
    val content = new StringBuilder();
    if (conn.getErrorStream() != null) {
      val in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))
      readAll(in, content)
      in.close()
    }

    val headers = collection.mutable.Map.empty[String, String]
    val statusLine = getResponseHeaders(conn, headers)

    HttpResponseData(statusLine, content.toString, headers)
  }

  /**
   * Read in headers
   *
   * @param conn HTTP Connection
   * @param headers Collection of header name-values
   */
  def getResponseHeaders(conn: URLConnection, headers: collection.mutable.Map[String, String]): String = {
    headers.clear()
    var statusLine = ""
    var i = 0;
    while (i >= 0) {
      val name = conn.getHeaderFieldKey(i)
      val value = conn.getHeaderField(i)

      if (name == null && value == null) {
        i = -1
      } else if (name == null) {
        // Value must be the response code like "HTTP/1.1 404 Not Found"
        statusLine = value
        i += 1
      } else {
        headers.put(name, value)
        i += 1
      }
    }

    statusLine
  }

  private def readAll(in: BufferedReader, sb: StringBuilder) = {
    var finished = false
    while (!finished) {
      val str = in.readLine()
      if (str == null) {
        sb.setLength(sb.length - 1) //Remove last \n
        finished = true
      } else {
        sb.append(str + "\n");
      }
    }
  }

  /**
   * Data returned from the HTTP connection
   */
  case class HttpResponseData(
    statusLine: String,
    content: String,
    headers: collection.mutable.Map[String, String]) {

    private val startIndex = statusLine.indexOf(" ") + 1
    val status = statusLine.substring(startIndex, startIndex + 3)

    override def toString(): String = {
      val sb = new StringBuilder
      sb.append(statusLine + "\n")
      headers.foreach(m => sb.append(m._1 + "=" + m._2 + "\n"))
      sb.append("\n" + content + "\n")
      sb.toString
    }

  }
}

