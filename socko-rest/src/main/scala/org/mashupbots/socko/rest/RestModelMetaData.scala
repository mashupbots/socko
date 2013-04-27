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

/**
 * Meta data to describe REST model classes. The trait is expected to be implemented by companion objects.
 * 
 * For example:
 * {{{
 * case class Pet(tags: Array[Tag], id: Long, category: Category, status: String, name: String, photoUrls: Array[String])
 * 
 * object Pet extends RestModelMetaData {
 *   val modelProperties = Seq(
 *     RestPropertyMetaData("status", "pet status in the store", Some(AllowableValuesList(List("available", "pending", "sold")))))
 * }
 * }}}
 */
trait RestModelMetaData {

  /**
   * Specifies additional property meta data in this model class
   */
  def modelProperties: Seq[RestPropertyMetaData]
}

/**
 * Describes a property in this model class
 *
 * @param name Name of field
 * @param description Brief description of the field
 * @param allowableValues Optional allowable list of values or range of values
 */
case class RestPropertyMetaData(
  name: String,
  description: String,
  allowableValues: Option[AllowableValues] = None)