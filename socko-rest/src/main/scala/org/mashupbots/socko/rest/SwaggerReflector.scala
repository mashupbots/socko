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

import scala.reflect.runtime.{ universe => ru }
import java.util.Date

/**
 * Scala to Swagger conversion
 */
object SwaggerReflector {
  val stringType = ru.typeOf[String]
  val optionStringType = ru.typeOf[Option[String]]

  val intType = ru.typeOf[Int]
  val optionIntType = ru.typeOf[Option[Int]]

  val booleanType = ru.typeOf[Boolean]
  val optionBooleanType = ru.typeOf[Option[Boolean]]

  val byteType = ru.typeOf[Byte]
  val optionByteType = ru.typeOf[Option[Byte]]

  val shortType = ru.typeOf[Short]
  val optionShortType = ru.typeOf[Option[Short]]

  val longType = ru.typeOf[Long]
  val optionLongType = ru.typeOf[Option[Long]]

  val doubleType = ru.typeOf[Double]
  val optionDoubleType = ru.typeOf[Option[Double]]

  val floatType = ru.typeOf[Float]
  val optionFloatType = ru.typeOf[Option[Float]]

  val dateType = ru.typeOf[Date]
  val optionDateType = ru.typeOf[Option[Date]]

  val anyRefType = ru.typeOf[AnyRef]
  val optionAnyRefType = ru.typeOf[Option[AnyRef]]

  val arrayType = ru.typeOf[Array[_]]
  val seqType = ru.typeOf[Seq[_]]
  val setType = ru.typeOf[Set[_]]

  /**
   * Converts a scala type of a swagger type
   *
   * @param tpe Scala type of a class or value
   * @return String description of the class or value. Empty string if it cannot be converted.
   */
  def dataType(tpe: ru.Type): String = {
    if (tpe =:= stringType || tpe =:= optionStringType) "string"
    else if (tpe =:= intType || tpe =:= optionIntType) "int"
    else if (tpe =:= booleanType || tpe =:= optionBooleanType) "boolean"
    else if (tpe =:= byteType || tpe =:= optionByteType) "byte"
    else if (tpe =:= shortType || tpe =:= optionShortType) "short"
    else if (tpe =:= longType || tpe =:= optionLongType) "long"
    else if (tpe =:= doubleType || tpe =:= optionDoubleType) "double"
    else if (tpe =:= floatType || tpe =:= optionFloatType) "float"
    else if (tpe =:= dateType || tpe =:= optionDateType) "date"
    else if (tpe <:< seqType || tpe <:< arrayType) {
      // Map all seq and array to a swagger array because I cannot find 
      // the scala equivalent for a ordered list
      // http://stackoverflow.com/questions/12842729/finding-type-parameters-via-reflection-in-scala-2-10
      // http://www.scala-lang.org/api/current/index.html#scala.reflect.api.Types$Type
      // tpe = List[Int]
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol] // class List
      val typeParams = cs.typeParams // List(type A)
      val firstTypeParam = typeParams(0).asType.toType // type A
      val genericType = firstTypeParam.asSeenFrom(tpe, cs)
      s"Array[${dataType(genericType)}]"
    } else if (tpe <:< setType) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val typeParams = cs.typeParams
      val firstTypeParam = typeParams(0).asType.toType
      val contentType = firstTypeParam.asSeenFrom(tpe, cs)
      s"Set[${dataType(contentType)}]"
    } else if (tpe <:< optionAnyRefType) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val typeParams = cs.typeParams
      val firstTypeParam = typeParams(0).asType.toType
      val contentType = firstTypeParam.asSeenFrom(tpe, cs)
      dataType(contentType)
    } else if (tpe <:< anyRefType) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val className = cs.fullName
      val dot = className.lastIndexOf('.')
      if (dot > 0) className.substring(dot + 1)
      else className
    } else ""
  }

  /**
   * Checks if the type is a primitive
   *
   * @param tpe Type to check
   * @return `True` if `tpe` is a primitive, `False` otherwise
   */
  def isPrimitive(tpe: ru.Type): Boolean = {
    if (tpe =:= stringType || tpe =:= optionStringType) true
    else if (tpe =:= intType || tpe =:= optionIntType) true
    else if (tpe =:= booleanType || tpe =:= optionBooleanType) true
    else if (tpe =:= byteType || tpe =:= optionByteType) true
    else if (tpe =:= shortType || tpe =:= optionShortType) true
    else if (tpe =:= longType || tpe =:= optionLongType) true
    else if (tpe =:= doubleType || tpe =:= optionDoubleType) true
    else if (tpe =:= floatType || tpe =:= optionFloatType) true
    else if (tpe =:= dateType || tpe =:= optionDateType) true
    else false
  }

  /**
   * Returns the type of container (if it is a container)
   *  - List. An ordered list of values is not supported because I cannot find Scala equivalent.
   *  - Set. An unordered set of unique values maps to a Scala Set
   *  - Array. An unordered list of values maps to a Scala Seq or Array
   *
   * @param tpe Type to check
   * @return `List`, `Array`, `Set` or empty string if not a container
   */
  def containerType(tpe: ru.Type): String = {
    if (tpe <:< seqType) "Array"
    else if (tpe <:< arrayType) "Array"
    else if (tpe <:< setType) "Set"
    else ""
  }

  /**
   * Returns the type of the contents of the container.
   *
   * For example, `typeOf[List[Pet]]` will return `typeOf[Pet]`
   *
   * @param tpe Container type to reflect
   * @return Content type of the container
   */
  def containerContentType(tpe: ru.Type): ru.Type = {
    val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
    val typeParams = cs.typeParams
    val firstTypeParam = typeParams(0).asType.toType
    val contentType = firstTypeParam.asSeenFrom(tpe, cs)
    contentType
  }

  /**
   * Determines if `tpe` is `typeOf[Option[_]]`
   *
   * @param tpe Type to check
   * @return `true` if option, `false` otherwise
   */
  def isOption(tpe: ru.Type): Boolean = (tpe <:< SwaggerReflector.optionAnyRefType)

  /**
   * Returns the type of the contents of the option.
   *
   * For example, `typeOf[Option[Pet]]` will return `typeOf[Pet]`
   *
   * @param tpe Option type to reflect
   * @return Content type of the option. If not an option, the input `tpe` is returned
   */
  def optionContentType(tpe: ru.Type): ru.Type = {
    if (isOption(tpe)) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val typeParams = cs.typeParams
      val firstTypeParam = typeParams(0).asType.toType
      val contentType = firstTypeParam.asSeenFrom(tpe, cs)
      contentType
    } else {
      tpe
    }
  }
}