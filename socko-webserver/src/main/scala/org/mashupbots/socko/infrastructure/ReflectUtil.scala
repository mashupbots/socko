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

import java.io.File
import java.net.URISyntaxException
import java.util.jar.JarFile

import scala.collection.JavaConversions._
import scala.reflect.runtime.{ universe => ru }

/**
 * Reflection utility functions
 */
object ReflectUtil extends Logger {

  /**
   * Returns a list of classes in the package and its sub-packages
   *
   * @param classLoader Class loaded to use to reflect classes. To make things simple, use a classloader
   *   of a class in the package you wish to reflect
   * @param packageName Name of package. For example, `org.mypackage`.
   * @see http://stackoverflow.com/questions/176527/how-can-i-enumerate-all-classes-in-a-package-and-add-them-to-a-list
   */
  def getClasses(classLoader: ClassLoader, packageName: String): List[Class[_]] = {

    val relPath = packageName.replace('.', '/')
    val resource = classLoader.getResource(relPath)
    if (resource == null) {
      throw new RuntimeException("No resource for " + relPath);
    }
    log.debug("Package: {} becomes Path: {} {}", packageName, relPath, "")

    val fullPath = resource.getFile()
    log.debug("Resource = {}", resource)

    val directory = try { new File(resource.toURI()) } catch {
      case ex: URISyntaxException =>
        throw new RuntimeException(packageName + " (" + resource + ") does not appear to be a valid URL / URI.", ex)
      case _: Throwable =>
        null
    }

    val classNames: List[String] = if (directory != null && directory.exists()) {
      // If the classes are in a directory (as opposed to a JAR file)
      val classFiles = IOUtil.recursiveListFiles(directory, """^[\d\w]+\.class$""".r).toList
      classFiles.map(f => {
        val fileName = f.getName()
        val className = packageName + "." + fileName.substring(0, fileName.length() - 6)
        log.debug("Found class in directory {}", className)
        className
      })
    } else {
      // If classes are in a JAR, need to look through the JAR
      val jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "")
      val jarFile = new JarFile(jarPath)
      val entries = jarFile.entries()
      val i = for (
        e <- entries;
        entryName = e.getName();
        className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
        if (className.startsWith(packageName) && className.matches("""^[.\d\w]+$"""))
      ) yield {
        log.debug("Found class in JAR {}", className)
        className
      }
      i.toList
    }

    // Convert 
    def getClass(className: String): Option[Class[_]] = {
      try {
        Some(Class.forName(className))
      } catch {
        case e: Throwable => {
          None
        }
      }
    }

    // Convert class names to classes
    val classes = for (
      className <- classNames;
      clz = getClass(className);
      if (clz.isDefined)
    ) yield clz.get
    classes.toList
  }

  /**
   * Retrieves the value of an argument in an annotation
   *
   * @param a Annotation
   * @param n Name of annotation parameter
   * @param defaultValue value to return if `n` is not found
   * @return Value of the annotation if found, `defaultValue` otherwise
   */
  def getAnnotationJavaLiteralArg[T](a: ru.Annotation, n: ru.Name, defaultValue: T): T = {
    val x = a.javaArgs
    val y = a.scalaArgs
    val arg = a.javaArgs.get(n)
    if (arg.isDefined) {
      arg.get.asInstanceOf[ru.LiteralArgument].value.value.asInstanceOf[T];
    } else {
      defaultValue
    }
  }
} 
