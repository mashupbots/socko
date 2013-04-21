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
  private val stringType = ru.typeOf[String]
  private val optionStringType = ru.typeOf[Option[String]]

  private val intType = ru.typeOf[Int]
  private val optionIntType = ru.typeOf[Option[Int]]

  private val booleanType = ru.typeOf[Boolean]
  private val optionBooleanType = ru.typeOf[Option[Boolean]]

  private val byteType = ru.typeOf[Byte]
  private val optionByteType = ru.typeOf[Option[Byte]]

  private val shortType = ru.typeOf[Short]
  private val optionShortType = ru.typeOf[Option[Short]]

  private val longType = ru.typeOf[Long]
  private val optionLongType = ru.typeOf[Option[Long]]

  private val doubleType = ru.typeOf[Double]
  private val optionDoubleType = ru.typeOf[Option[Double]]

  private val floatType = ru.typeOf[Float]
  private val optionFloatType = ru.typeOf[Option[Float]]

  private val dateType = ru.typeOf[Date]
  private val optionDateType = ru.typeOf[Option[Date]]

  private val anyRefType = ru.typeOf[AnyRef]
  private val optionAnyRefType = ru.typeOf[Option[AnyRef]]

  private val arrayType = ru.typeOf[Array[_]]
  private val seqType = ru.typeOf[Seq[_]]
  private val setType = ru.typeOf[Set[_]]

  private val headTerm = ru.newTermName("head")

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