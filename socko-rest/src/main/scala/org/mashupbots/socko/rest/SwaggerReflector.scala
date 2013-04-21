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
   * @returns String description of the class or value. Empty string if it cannot be converted.
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
    else if (tpe <:< seqType) {
      //http://stackoverflow.com/questions/12842729/finding-type-parameters-via-reflection-in-scala-2-10
      //http://www.scala-lang.org/api/current/index.html#scala.reflect.api.Types$Type
      // tpe = List[Int]
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol] // class List
      val typeParams = cs.typeParams // List(type A)
      val firstTypeParam = typeParams(0).asType.toType // type A
      val genericType = firstTypeParam.asSeenFrom(tpe, cs)
      s"List[${dataType(genericType)}]"
    } else if (tpe <:< arrayType) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val typeParams = cs.typeParams
      val firstTypeParam = typeParams(0).asType.toType
      val genericType = firstTypeParam.asSeenFrom(tpe, cs)
      s"Array[${dataType(genericType)}]"
    } else if (tpe <:< setType) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val typeParams = cs.typeParams
      val firstTypeParam = typeParams(0).asType.toType
      val genericType = firstTypeParam.asSeenFrom(tpe, cs)
      s"Set[${dataType(genericType)}]"
    } else if (tpe <:< optionAnyRefType) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val typeParams = cs.typeParams
      val firstTypeParam = typeParams(0).asType.toType
      val genericType = firstTypeParam.asSeenFrom(tpe, cs)
      dataType(genericType)
    } else if (tpe <:< anyRefType) {
      val cs = tpe.typeSymbol.asInstanceOf[ru.ClassSymbol]
      val className = cs.fullName
      val dot = className.lastIndexOf('.')
      if (dot > 0) className.substring(dot + 1)
      else className
    } else ""
  }
}