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
package org.mashupbots.socko.examples.querystring_post

import org.mashupbots.socko.events.HttpRequestEvent
import akka.actor.Actor
import java.util.Date

/**
 * Hello processor writes a greeting and stops.
 */
class QueryStringPostHandler extends Actor {
  def receive = {
    
    //
    // Home page
    //
    case HomePage(event) => {

      val buf = new StringBuilder()
      buf.append("<html>\n")
      buf.append("<head>\n")
      buf.append("  <title>Socko Parse Query String and Post Data Example</title>\n")
      buf.append("</head>\n")
      buf.append("<body>\n")

      buf.append("<h1>Socko Parse Query String Example</h1>\n")
      buf.append("<p>Click <a href=\"/querystring?a=1&b=2\">here</a> to parse <tt>?a=1&b=2</tt>.</p>\n")

      buf.append("<h1>Socko Parse Post Data Example</h1>\n")
      buf.append("<form action=\"/post\" enctype=\"application/x-www-form-urlencoded\" method=\"post\">\n")

      buf.append("  <div>\n")
      buf.append("    <label>1. First Name</label><br/>\n")
      buf.append("    <input type=\"text\" name=\"firstName\" size=\"50\" />\n")
      buf.append("  </div>\n")

      buf.append("  <div>\n")
      buf.append("    <label>2. Last Name</label><br/>\n")
      buf.append("    <input type=\"text\" name=\"lastName\" size=\"50\" />\n")
      buf.append("  </div>\n")

      buf.append("  <div style=\"margin-top:10px;\">\n")
      buf.append("    <input type=\"submit\" value=\"Post\" />\n")
      buf.append("  </div>\n")

      buf.append("</form>\n")
      buf.append("</body>\n")
      buf.append("</html>\n")
      
      event.response.contentType = "text/html"
      event.response.write(buf.toString)
      context.stop(self)
    }

    //
    // Parse query string
    //
    case ShowQueryStringDataPage(event) => {
      val qsMap = event.endPoint.queryStringMap

      val data = qsMap.foldLeft("")((accumulation, entry) => {
        val (key, values) = entry
        accumulation + s"\nname=$key value=${values(0)}"
      })

      event.response.write("Query String Data\n" + data)
      context.stop(self)
    }

    //
    // Parse application/x-www-form-urlencoded post data
    //
    case ShowPostDataPage(event) => {
      val formDataMap = event.request.content.toFormDataMap
      
      val data = formDataMap.foldLeft("")((accumulation, entry) => {
        val (key, values) = entry
        accumulation + s"\nname=$key value=${values(0)}"
      })
            
      event.response.write("Form Data\n" + data)
      context.stop(self)
    }
  }
}

case class HomePage(event: HttpRequestEvent)
case class ShowQueryStringDataPage(event: HttpRequestEvent)
case class ShowPostDataPage(event: HttpRequestEvent)


