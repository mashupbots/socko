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
package org.mashupbots.socko.rest

import java.io.InputStream
import java.net.URL
import java.util.Date
import scala.reflect.runtime.{ universe => ru }
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.json4s.NoTypeHints
import org.json4s.native.{ Serialization => json }
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.DateUtil
import org.mashupbots.socko.infrastructure.IOUtil
import java.io.BufferedInputStream

/**
 * Seralized outgoing data from a [[org.mashupbots.socko.rest.RestResposne]]
 *
 * @param responseClass Response class symbol
 */
case class RestResponseSerializer(
  responseClass: ru.ClassSymbol,
  responseDataType: ResponseDataType.Value,
  responseDataTerm: Option[ru.TermSymbol],
  rm: ru.Mirror) {

  /**
   * Returns the data object for an instance of the response
   *
   * @returns `None` if the response data type is void, else the value of the data field
   */
  def getData(response: RestResponse): Any = {
    if (responseDataType == ResponseDataType.Void) null
    else {
      val instanceMirror = rm.reflect(response)
      val fieldMirror = instanceMirror.reflectField(responseDataTerm.get)
      fieldMirror.get
    }
  }

  /**
   * Serialize the data to a HTTP response
   *
   * @param http HTTP event
   * @param response Response object to serailize
   */
  def serialize(http: HttpRequestEvent, response: RestResponse) {
    responseDataType match {
      case ResponseDataType.Object => {
        val data = getData(response)
        val bytes = {
          implicit val formats = json.formats(NoTypeHints)
          val s = json.write(data.asInstanceOf[AnyRef])
          s.getBytes(CharsetUtil.UTF_8)
        }
        http.response.write(response.context.status, bytes, "application/json; charset=UTF-8", response.context.headers)
      }
      case ResponseDataType.ByteArray => {
        val data = getData(response)
        val bytes = data.asInstanceOf[Array[Byte]]
        val contentType = response.context.headers.getOrElse(HttpHeaders.Names.CONTENT_TYPE, "application/octet-string")
        http.response.write(response.context.status, bytes, contentType, response.context.headers)
      }
      case ResponseDataType.URL => {
        val data = getData(response)
        val url = data.asInstanceOf[URL]
        val contentType = response.context.headers.getOrElse(HttpHeaders.Names.CONTENT_TYPE, "application/octet-string")
        http.response.writeFirstChunk(response.context.status, contentType, response.context.headers)

        // TO DO use chunk writers to be more efficient and non blocking
        val buf = new Array[Byte](8192)
        IOUtil.using(new BufferedInputStream(url.openStream())) { r =>
          def doPipe(): Unit = {
            val bytesRead = r.read(buf)
            if (bytesRead > 0) {
              val w = if (bytesRead == buf.length) buf else buf.slice(0, bytesRead)
              http.response.writeChunk(buf)
              doPipe()
            }
          }
          doPipe()
        }

        http.response.writeLastChunk()
      }
      case ResponseDataType.Primitive => {
        val data = getData(response)
        val bytes = data.toString.getBytes(CharsetUtil.UTF_8)
        http.response.write(response.context.status, bytes, "text/plain; charset=UTF-8", response.context.headers)
      }
      case ResponseDataType.Void => {
        http.response.write(response.context.status, Array.empty[Byte], "", response.context.headers)
      }
      case _ => {
        throw new IllegalStateException(s"Unsupported ResponseDataType ${responseDataType.toString}")
      }
    }
  }

}

/**
 * Companion object
 */
object RestResponseSerializer {

  private val primitiveTypes: Map[ru.Type, (Any) => String] = Map(
    (ru.typeOf[String], (s: Any) => s.toString),
    (ru.typeOf[Int], (s: Any) => s.toString),
    (ru.typeOf[Boolean], (s: Any) => s.toString),
    (ru.typeOf[Byte], (s: Any) => s.toString),
    (ru.typeOf[Short], (s: Any) => s.toString),
    (ru.typeOf[Long], (s: Any) => s.toString),
    (ru.typeOf[Double], (s: Any) => s.toString),
    (ru.typeOf[Float], (s: Any) => s.toString),
    (ru.typeOf[Date], (s: Any) => DateUtil.formatISO8601DateTime(s.asInstanceOf[Date])))

  private val inputStreamType = ru.typeOf[InputStream]

  /**
   * Factory for RestResponseSerializer
   *
   * @param rm Runtime Mirror with the same class loaders as the specified request class
   * @param definition Definition of the operation
   * @param responseClassSymbol Response class symbol
   */
  def apply(rm: ru.Mirror, definition: RestOperationDef, responseClassSymbol: ru.ClassSymbol): RestResponseSerializer = {
    val responseConstructor: ru.MethodSymbol = responseClassSymbol.toType.declaration(ru.nme.CONSTRUCTOR).asMethod
    val responseConstructorParams: List[ru.TermSymbol] = responseConstructor.paramss(0).map(p => p.asTerm)

    if (responseConstructorParams.size == 0) {
      throw RestDefintionException(s"${responseClassSymbol.fullName} constructor must have parameters.")
    } else if (responseConstructorParams(0).name.toString() != "context") {
      throw RestDefintionException(s"First constructor parameter for ${responseClassSymbol.fullName} must be termed 'context'.")
    }

    val responseDataType = if (responseConstructorParams.size == 1) {
      ResponseDataType.Void
    } else {
      val contentTerm = responseConstructorParams(1)
      val contentType = contentTerm.typeSignature
      if (primitiveTypes.exists(t => t._1 =:= contentType)) {
        ResponseDataType.Primitive
      } else if (contentType =:= ru.typeOf[Array[Byte]]) {
        ResponseDataType.ByteArray
      } else if (contentType =:= ru.typeOf[URL]) {
        ResponseDataType.URL
      } else if (contentType <:< ru.typeOf[AnyRef]) {
        ResponseDataType.Object
      } else {
        throw new IllegalArgumentException(s"Unsupported REST response data type ${contentType} in ${responseClassSymbol.fullName}.")
      }
    }

    // The data term name assumed to be in the constructor of a "case class"
    // Get the term name and reflect it as a field in order to read its value
    val responseDataTerm: Option[ru.TermSymbol] = if (responseDataType == ResponseDataType.Void) None else {
      val contentTerm = responseConstructorParams(1)
      Some(responseClassSymbol.toType.declaration(contentTerm.name).asTerm.accessed.asTerm)
    }

    RestResponseSerializer(responseClassSymbol, responseDataType, responseDataTerm, rm)
  }

}

object ResponseDataType extends Enumeration {
  type ResponseDataType = Value
  val Void, Primitive, Object, ByteArray, URL = Value
} 

  