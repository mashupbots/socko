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
package org.mashupbots.socko.examples.fileupload

import org.mashupbots.socko.context.HttpRequestProcessingContext
import akka.actor.Actor
import java.util.Date
import java.io.File
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.mashupbots.socko.postdecoder.HttpPostRequestDecoder
import org.mashupbots.socko.postdecoder.DefaultHttpDataFactory
import scala.collection.JavaConversions._
import org.mashupbots.socko.postdecoder.InterfaceHttpData.HttpDataType
import org.mashupbots.socko.postdecoder.Attribute
import org.mashupbots.socko.postdecoder.FileUpload
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import akka.event.Logging

/**
 * Processes file uploads
 */
class FileUploadProcessor extends Actor {

  private val log = Logging(context.system, this)

  def receive = {
    case request: FileUploadRequest => {
      val ctx = request.context
      try {
        val contentType = ctx.getHeader(HttpHeaders.Names.CONTENT_TYPE)
        if (contentType.isDefined &&
          (contentType.get.startsWith("multipart/form-data") ||
            contentType.get.startsWith("application/x-www-form-urlencoded"))) {

          val decoder = new HttpPostRequestDecoder(HttpDataFactory.value, ctx.httpRequest)

          val descriptionField = decoder.getBodyHttpData("fileDescription").asInstanceOf[Attribute]

          val uploadField = decoder.getBodyHttpData("fileUpload").asInstanceOf[FileUpload]
          val destFile = new File(request.saveDir, uploadField.getFilename)
          uploadField.renameTo(destFile)

          val html = buildResponseHtml(descriptionField.getValue, destFile)
          ctx.writeResponse(html, "text/html")
        } else {
          ctx.writeErrorResponse(HttpResponseStatus.BAD_REQUEST)
        }
      } catch {
        case ex => {
          ctx.writeErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, msg = ex.toString)          
        }
      }
    }
  }

  private def buildResponseHtml(description: String, newFile: File): String = {
    val buf = new StringBuilder()
    buf.append("<html>\n")
    buf.append("<head>\n")
    buf.append("  <title>Socko File Upload Example</title>\n")
    buf.append("  <link rel=\"stylesheet\" type=\"text/css\" href=\"mystyle.css\" />\n")
    buf.append("</head>\n")
    buf.append("<body>\n")
    buf.append("<h1>Socko File Upload Example</h1>\n")
    buf.append("<p>\n")
    buf.append("  File uploaded to: " + newFile.getCanonicalPath)
    buf.append("</p>\n")
    buf.append("<p>\n")
    buf.append("  File description: " + description)
    buf.append("</p>\n")
    buf.append("<p>\n")
    buf.append("  <a href=\"/index.html\">Back</a>")
    buf.append("</p>\n")
    buf.append("</html>\n")

    buf.toString
  }
}

case class FileUploadRequest(
  context: HttpRequestProcessingContext,
  saveDir: File)

/**
 * Data factory for use with `HttpPostRequestDecoder`
 */
object HttpDataFactory {
  val value = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE)
}

