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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import java.util.Date
import java.util.GregorianCalendar
import java.net.InetSocketAddress
import org.scalatest.GivenWhenThen
import java.util.TimeZone
import java.util.Calendar
import java.text.ParseException
import scala.reflect.runtime.{universe => ru}

class UtilSpec extends WordSpec with ShouldMatchers with GivenWhenThen with Logger {

  "DateUtil" should {
    "format dates" in {
      val cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"))
      cal.set(2012, 5, 6, 10, 20, 30)

      val s = DateUtil.rfc1123DateFormatter.format(cal.getTime)
      s should be("Wed, 06 Jun 2012 10:20:30 GMT")
    }

    "parse dates" in {
      val cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"))
      cal.set(2012, 5, 6, 10, 20, 30)
      cal.set(Calendar.MILLISECOND, 0)

      val d = DateUtil.rfc1123DateFormatter.parse("Wed, 06 Jun 2012 10:20:30 GMT")
      d.getTime should equal(cal.getTime.getTime)
    }

    "throw exception with invalid dates" in {
      val ex = intercept[ParseException] {
        DateUtil.rfc1123DateFormatter.parse("2010-1-1 10:20:30")
      }
    }
  }

  "IOUtil" should {
    "read resource" in {
      val s = IOUtil.readResource("META-INF/mime.types")
      //System.out.println(new String(s))
      s.length should be > 0
    }

    "return null resource not found" in {
      IOUtil.readResource("not/found") should be(null)
    }

    "test for absolute paths" in {
      IOUtil.isAbsolutePath("/tmp") should be(true)
      IOUtil.isAbsolutePath("relative/1/2/3") should be(false)
      IOUtil.isAbsolutePath("c:\\") should be(true)
      IOUtil.isAbsolutePath("c:\\test") should be(true)
    }
  }

  "HashUtil" should {
    "md5 hash some bytes" in {
      val s = HashUtil.md5("some random text")
      s should be("07671a038c0eb43723d421693b073c3b")
    }
  }

  "MimeTypes" should {
    "identify common file type" in {
      MimeTypes.get("test.html") should be("text/html")
      MimeTypes.get("test.js") should be("application/javascript")
      MimeTypes.get("test.txt") should be("text/plain")
      MimeTypes.get("test.css") should be("text/css")
    }
  }

  "ReflectUtils" should {
    "find classes in a JAR" in {
      val annotations = ReflectUtil.getClasses(getClass().getClassLoader(), "scala.annotation")
      annotations.length should be > 0
      annotations.foreach(c => log.debug("Reflected class in JAR {}", c.getName()))
      
      val mirror = ru.runtimeMirror(getClass.getClassLoader)
      annotations.foreach(c => {
        val cc = mirror.classSymbol(c)
        log.debug("Mirror class {}. Annotations: {} {}", c.getName(), cc.annotations)
      })
    }
    
    "find classes in a directory" in {
      val clz = ReflectUtil.getClasses(getClass().getClassLoader(), "org.mashupbots.socko.infrastructure")
      clz.length should be > 0
      clz.foreach(c => log.debug("Reflected class in directory {}", c.getName()))
    }
    
  }
}
