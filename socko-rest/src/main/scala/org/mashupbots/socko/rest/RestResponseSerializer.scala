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

import java.util.Date

import scala.reflect.runtime.{universe => ru}

import org.jboss.netty.handler.codec.http.HttpHeaders
import org.json4s.NoTypeHints
import org.json4s.native.{Serialization => json}
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.infrastructure.CharsetUtil
import org.mashupbots.socko.infrastructure.Logger

/**
 * Serialized outgoing data from a [[org.mashupbots.socko.rest.RestResponse]]
 *
 * @param config REST configuration
 * @param responseClass Response class symbol
 * @param dataSerializer Data type specific serializer
 */
case class RestResponseSerializer(
  config: RestConfig,
  responseClass: ru.ClassSymbol,
  dataSerializer: DataSerializer) extends Logger {

  /**
   * Serialize the data to a HTTP response
   *
   * HEAD method does not have a body. This method can be used for obtaining meta information about the
   * entity implied by the request without transferring the entity-body itself. Hence, we map HEAD to
   * a Void response data type.
   *
   * @param http HTTP event
   * @param response Response object to serailize
   */
  def serialize(http: HttpRequestEvent, response: RestResponse) {
    val status = response.context.status
    val data = dataSerializer.getData(response)
    val bytes = dataSerializer.serialize(data)
    val contentType = {
      if (dataSerializer.forceContentType) dataSerializer.contentType
      else response.context.headers.getOrElse(HttpHeaders.Names.CONTENT_TYPE, dataSerializer.contentType)
    }
    http.response.write(status, bytes, contentType, response.context.headers)
  }
}

/**
 * Companion object
 */
object RestResponseSerializer {

  private val byteArrayType = ru.typeOf[Array[Byte]]
  private val byteSeqType = ru.typeOf[Seq[Byte]]
  private val anyRefType = ru.typeOf[AnyRef]

  /**
   * Factory for RestResponseSerializer
   *
   * @param config REST config
   * @param rm Runtime Mirror with the same class loaders as the specified request class
   * @param registration REST operation registration details
   * @param responseClassSymbol Response class symbol
   */
  def apply(config: RestConfig, rm: ru.Mirror, registration: RestRegistration, responseClassSymbol: ru.ClassSymbol): RestResponseSerializer = {
    val responseConstructor: ru.MethodSymbol = responseClassSymbol.toType.declaration(ru.nme.CONSTRUCTOR).asMethod
    val responseConstructorParams: List[ru.TermSymbol] = responseConstructor.paramss(0).map(p => p.asTerm)

    if (responseConstructorParams.size == 0) {
      throw RestDefintionException(s"'${responseClassSymbol.fullName}' constructor must have parameters.")
    } else if (responseConstructorParams(0).name.toString() != "context") {
      throw RestDefintionException(s"First constructor parameter of '${responseClassSymbol.fullName}' must be called 'context'.")
    }

    val dataSerializer = if (responseConstructorParams.size == 1) {
      VoidDataSerializer()
    } else {
      // Instance the correct data serializer
      val paramDataTerm = responseConstructorParams(1)
      val paramDataType = paramDataTerm.typeSignature

      // The data term name assumed to be in the constructor of a "case class"
      // Get the term name and reflect it as a field in order to read its value in getData()
      val responseDataTerm = responseClassSymbol.toType.declaration(paramDataTerm.name).asTerm.accessed.asTerm

      val dataSerializer = if (PrimitiveDataSerializer.IsPrimitiveDataType(paramDataType)) {
        PrimitiveDataSerializer(paramDataType, responseDataTerm, rm)
      } else if (paramDataType <:< byteSeqType) {
        ByteSeqDataSerializer(responseDataTerm, rm)
      } else if (paramDataType =:= byteArrayType) {
        ByteArrayDataSerializer(responseDataTerm, rm)
      } else if (paramDataType <:< anyRefType) {
        ObjectDataSerializer(paramDataType, responseDataTerm, rm)
      } else {
        throw new IllegalArgumentException(s"Unsupported REST response data type ${paramDataType} in ${responseClassSymbol.fullName}.")
      }

      dataSerializer
    }

    // Finish up
    RestResponseSerializer(config, responseClassSymbol, dataSerializer)
  }

}

/**
 * Serializes data into a byte array
 */
abstract class DataSerializer {
  /**
   * Converts an object into JSON encoded in UTF-8
   *
   * @param data Data to serialize
   * @return Array of bytes representing the serialized data
   */
  def serialize(data: Any): Array[Byte]

  /**
   * Content MIME Type
   */
  def contentType: String

  /**
   * Force the use of `ContentType` even if it is specified in the headers
   */
  def forceContentType: Boolean

  /**
   * Returns the data object for an instance of the response
   *
   * @return `None` if the response data type is void, else the value of the data field
   */
  def getData(response: RestResponse): Any
}

/**
 * Encapsulates common aspects of non void serializers
 */
abstract class NonVoidDataSerializer() extends DataSerializer {
  /**
   * Name of field used to store response data.
   * For `case class StringResponse(context: RestResponseContext, data: String) extends RestResponse`,
   * the term is `data`.
   */
  val responseDataTerm: ru.TermSymbol

  /**
   * Mirror used to extract the value of `responseDataTerm` from the response object
   */
  val rm: ru.Mirror

  /**
   * Returns the data object for an instance of the response
   *
   * @return `None` if the response data type is void, else the value of the data field
   */
  def getData(response: RestResponse): Any = {
    val instanceMirror = rm.reflect(response)
    val fieldMirror = instanceMirror.reflectField(responseDataTerm)
    fieldMirror.get
  }
}

/**
 * Serializes a void response.
 *
 * This is a placeholder because with `void`, there is no data to serialize.
 */
case class VoidDataSerializer() extends DataSerializer {

  def getData(response: RestResponse): Any = null

  def serialize(data: Any): Array[Byte] = Array.empty

  val contentType = ""

  val forceContentType = false
}

/**
 * Serialize an object into a UTF-8 JSON byte array
 *
 * If the data is of type `Option[]` and the value is None, an empty array will be returned.
 *
 * @param tpe Type of the data to serialize
 * @param responseDataTerm Name of field used to store response data.
 *   For `case class StringResponse(context: RestResponseContext, data: String) extends RestResponse`,
 *   the term is `data`.
 * @param rm Mirror used to extract the value of `responseDataTerm` from the response object
 */
case class ObjectDataSerializer(
  tpe: ru.Type,
  responseDataTerm: ru.TermSymbol,
  rm: ru.Mirror) extends NonVoidDataSerializer {

  def serialize(data: Any): Array[Byte] = {
    if (data == null) Array.empty
    else {
      implicit val formats = json.formats(NoTypeHints)
      val s = json.write(data.asInstanceOf[AnyRef])
      s.getBytes(CharsetUtil.UTF_8)
    }
  }

  val contentType = "application/json; charset=UTF-8"

  val forceContentType = true
}

/**
 * Serialize a primitive into a UTF-8 JSON byte array
 *
 * If the data is of type `Option[]` and the value is None, an empty array will be returned.
 *
 * @param tpe Type of the data to serialize
 * @param responseDataTerm Name of field used to store response data.
 *   For `case class StringResponse(context: RestResponseContext, data: String) extends RestResponse`,
 *   the term is `data`.
 * @param rm Mirror used to extract the value of `responseDataTerm` from the response object
 */
case class PrimitiveDataSerializer(
  tpe: ru.Type,
  responseDataTerm: ru.TermSymbol,
  rm: ru.Mirror) extends NonVoidDataSerializer {

  private val entry = PrimitiveDataSerializer.primitiveTypes.find(e => e._1 =:= tpe)
  private val serializer = if (entry.isDefined) {
    val (t, serializer) = entry.get
    serializer
  } else {
    throw new RestBindingException("Unsupported type: " + tpe)
  }

  def serialize(data: Any): Array[Byte] = {
    serializer(data)
  }

  val contentType = "application/json; charset=UTF-8"

  val forceContentType = true
}

/**
 * Companion class
 */
object PrimitiveDataSerializer {
  private def jsonifyString(s: Any): Array[Byte] = {
    implicit val formats = json.formats(NoTypeHints)
    json.write(s.asInstanceOf[String]).getBytes(CharsetUtil.UTF_8)
  }
  private def jsonifyOptionString(s: Any): Array[Byte] = {
    val ss = s.asInstanceOf[Option[String]]
    if (ss.isEmpty) Array.empty
    else jsonifyString(ss.get)
  }
  private def jsonifyVal(a: Any): Array[Byte] = {
    a.toString.getBytes(CharsetUtil.UTF_8)
  }
  private def jsonifyOptionVal(a: Any): Array[Byte] = {
    val ss = a.asInstanceOf[Option[_]]
    if (ss.isEmpty) Array.empty
    else ss.get.toString.getBytes(CharsetUtil.UTF_8)
  }
  private def jsonifyDate(d: Any): Array[Byte] = {
    implicit val formats = json.formats(NoTypeHints)
    json.write(d.asInstanceOf[Date]).getBytes(CharsetUtil.UTF_8)
  }
  private def jsonifyOptionDate(a: Any): Array[Byte] = {
    val ss = a.asInstanceOf[Option[Date]]
    if (ss.isEmpty) Array.empty
    else jsonifyDate(ss.get)
  }

  val primitiveTypes: Map[ru.Type, (Any) => Array[Byte]] = Map(
    (ru.typeOf[String], (s: Any) => jsonifyString(s)),
    (ru.typeOf[Option[String]], (s: Any) => jsonifyOptionString(s)),
    (ru.typeOf[Int], (s: Any) => jsonifyVal(s)),
    (ru.typeOf[Option[Int]], (s: Any) => jsonifyOptionVal(s)),
    (ru.typeOf[Boolean], (s: Any) => jsonifyVal(s)),
    (ru.typeOf[Option[Boolean]], (s: Any) => jsonifyOptionVal(s)),
    (ru.typeOf[Byte], (s: Any) => jsonifyVal(s)),
    (ru.typeOf[Option[Byte]], (s: Any) => jsonifyOptionVal(s)),
    (ru.typeOf[Short], (s: Any) => jsonifyVal(s)),
    (ru.typeOf[Option[Short]], (s: Any) => jsonifyOptionVal(s)),
    (ru.typeOf[Long], (s: Any) => jsonifyVal(s)),
    (ru.typeOf[Option[Long]], (s: Any) => jsonifyOptionVal(s)),
    (ru.typeOf[Double], (s: Any) => jsonifyVal(s)),
    (ru.typeOf[Option[Double]], (s: Any) => jsonifyOptionVal(s)),
    (ru.typeOf[Float], (s: Any) => jsonifyVal(s)),
    (ru.typeOf[Option[Float]], (s: Any) => jsonifyOptionVal(s)),
    (ru.typeOf[Date], (s: Any) => jsonifyDate(s)),
    (ru.typeOf[Option[Date]], (s: Any) => jsonifyOptionDate(s)))

  def IsPrimitiveDataType(tpe: ru.Type): Boolean = {
    primitiveTypes.exists(p => p._1 =:= tpe)
  }
}

/**
 * Serialize a byte array (hint: this does not do much!)
 *
 * @param responseDataTerm Name of field used to store response data.
 *   For `case class StringResponse(context: RestResponseContext, data: String) extends RestResponse`,
 *   the term is `data`.
 * @param rm Mirror used to extract the value of `responseDataTerm` from the response object
 */
case class ByteArrayDataSerializer(
  responseDataTerm: ru.TermSymbol,
  rm: ru.Mirror) extends NonVoidDataSerializer {

  def serialize(data: Any): Array[Byte] = {
    if (data == null) Array.empty else data.asInstanceOf[Array[Byte]]
  }

  val contentType = "application/octet-string"

  val forceContentType = false
}

/**
 * Serialize a byte sequence (hint: this does not do much!)
 *
 * @param responseDataTerm Name of field used to store response data.
 *   For `case class StringResponse(context: RestResponseContext, data: String) extends RestResponse`,
 *   the term is `data`.
 * @param rm Mirror used to extract the value of `responseDataTerm` from the response object
 */
case class ByteSeqDataSerializer(
  responseDataTerm: ru.TermSymbol,
  rm: ru.Mirror) extends NonVoidDataSerializer {

  def serialize(data: Any): Array[Byte] = {
    if (data == null) Array.empty else (data.asInstanceOf[Seq[Byte]].toArray)
  }

  val contentType = "application/octet-string"

  val forceContentType = false
}