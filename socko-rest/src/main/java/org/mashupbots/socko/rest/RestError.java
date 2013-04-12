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
package org.mashupbots.socko.rest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes the error associated with a HTTP status
 * 
 * This is used in a nested fashion.
 * 
 * @see https://blogs.oracle.com/toddfast/entry/creating_nested_complex_java_annotations
 * @see http://stackoverflow.com/questions/3376441/how-to-use-new-scala-2-8-0-nested-annotations
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestError {
	/**
	 * HTTP error response code
	 */
	int code();

	/**
	 * Description of the error
	 */
	String reason();
}