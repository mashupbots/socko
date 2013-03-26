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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP GET REST end point annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RestGet {
	/**
	 * Template URL to use for matching to this request
	 */
	String urlTemplate();

	/**
	 * Class path of the processor actor locator class for this request. The class locates the actor
	 * to process the request.
	 * 
	 * <p>
	 * If empty, the following conventions are followed:
	 * <ol>
	 * <li>
	 *   Same class path and name as the request class; but with <tt>Request</tt> suffix replaced with 
	 *   <tt>ProcessorLocator</tt>. For <tt>MyRestRequest</tt>, the processor locator class would be 
	 *   <tt>MyRestProcessorLocator</tt>.
	 * </li>
	 * <li>
	 *   Same class path and name as the request class; but with <tt>Request</tt> suffix replaced with 
	 *   <tt>Processor</tt>. For <tt>MyRestRequest</tt>, the processor locator class would be 
	 *   <tt>MyRestProcessor</tt>.
	 * </li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * The class must extend <tt>org.mashupbots.socko.rest.RestProcessorLocator</tt>.  It can be a singleton object
	 * or an instance class with a parameterless constructor.
	 * </p>
	 */
	String processorLocatorClass() default "";
	
	/**
	 * Class path of the response class.
	 * 
	 * If empty, the assumed response class has the same class path and name as
	 * the request class; but with <tt>Request</tt> suffix replaced with <tt>Response</tt>.
	 * 
	 * For <tt>MyRestRequest</tt>, the default response class would be <tt>MyRestResponse</tt>.
	 */
	String responseClass() default "";

	/**
	 * Optional name used for the convenience of the UI and client code generator. 
	 * 
	 * If empty, the name of the request class without the `Request` suffix 
	 * will be used.
	 */
	String name() default "";

	/**
	 * Optional short description. Less than 60 characters is recommended.
	 */
	String description() default "";

	/**
	 * Optional long description
	 */
	String notes() default "";

	/**
	 * Flag to indicate if this operation is depreciated or not. 
	 * 
	 * Defaults to <tt>false</tt>.
	 */
	boolean depreciated() default false;

	/**
	 * Array of error responses in the format <tt>code=reason</tt>
	 * 
	 * For example: <tt>400=Username not found</tt>.
	 */
	String[] errorResponses() default {};
}