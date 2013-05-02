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

import akka.actor.Extension
import com.typesafe.config.Config
import org.mashupbots.socko.infrastructure.ConfigUtil
import java.net.URI

/**
 * Configuration for REST handler
 *
 * This can also be loaded from an externalized AKKA configuration file. For example:
 * 
 * {{{
 *   rest-config {
 *     # The version of your API. Required.
 *     api-version="1.0"
 *     
 *     # Root path to your API with the scheme, domain and port. Required. 
 *     # This is the path as seen by the end user and not from on the local server.
 *     root-api-url=http://yourdomain.com/api
 *     
 *     # Swagger definition version. Defaults to `1.1` if setting is omitted.
 *     swagger-version="1.1"
 *     
 *     # Path segments to group your APIs into Swagger resources. For exmaple, `/pet` is one resource
 *     # while `/user` is another. Default is `1` which refers to the first relative path segment.
 *     swagger-api-grouping-path-segment=1
 *     
 *     # Number of seconds before a request is timed out. 
 *     # Defaults to `60` seconds if setting is omitted.
 *     request-timeout-seconds=60
 *     
 *     # Number of seconds before a SockoEvent is removed from the cache and cannot be accessed by 
 *     # your actor. Defaults to `5` if setting is omitted.
 *     socko-event-cache-timeout-seconds=5
 *     
 *     # Maximum number of workers per RestHandler
 *     # Defaults to 100 if setting is omitted.
 *     max-worker-count=100
 *     
 *     # Reschedule a message for processing again using this delay when max worker count has been reached.
 *     # Defaults to 500 if setting is omitted 
 *     max-worker-reschedule-milliseconds=500
 *     
 *     # Determines if the message from runtime exceptions caught during handing of a REST request is returned   
 *     # to the caller in addition to the HTTP status code. Values are: `Never`, `BadRequestsOnly`, 
 *     # `InternalServerErrorOnly or `All`. 
 *     # Defaults to `Never` if setting is omitted 
 *     report-runtime-exception=Never
 *   }
 * }}}
 * 
 * can be loaded as follows:
 * {{{
 *   object MyRestHandlerConfig extends ExtensionId[RestConfig] with ExtensionIdProvider {
 *     override def lookup = MyRestHandlerConfig
 *     override def createExtension(system: ExtendedActorSystem) =
 *       new RestConfig(system.settings.config, "rest-config")
 *   }
 *
 *   val myRestConfig = MyRestHandlerConfig(actorSystem)
 * }}} 
 *  
 * @param apiVersion the version of your API
 * @param rootApiUrl Root path to your API with the scheme, domain and port. For example, `http://yourdomain.com/api`.
 *   This is the path as seen by the end user and not from on the local server.
 * @param swaggerVersion Swagger definition version
 * @param swaggerApiGroupingPathSegment Path segments to group APIs by. Default is `1` which refers to the first
 *   relative path segment.
 *
 *   For example, the following will be grouped under the `/pets` because the the share `pets` in the 1st path
 *   segment.
 *   {{{
 *   /pets
 *   /pets/{petId}
 *   /pets/findById
 *   }}}
 *
 * @param requestTimeoutSeconds Number of seconds before a request is timed out. Make sure that your processor
 *   actor responds within this number of seconds or throws an exception.  Defaults to `60` seconds.
 * @param sockoEventCacheTimeoutSeconds Number of seconds before a [[org.mashupbots.socko.events.SockoEvent]] is
 *   removed from the cache and cannot be accessed by the REST processor. Once the REST processor has access to
 *   the [[org.mashupbots.socko.events.SockoEvent]], its expiry from the cache does not affect usability. The cache
 *   is just used as a means to pass the event. Defaults to `5` seconds.
 * @param maxWorkerCount Maximum number of workers per [[org.mashupbots.socko.rest.RestHandler]].
 * @param maxWorkerRescheduleMilliSeconds Reschedule a message for processing again using this delay when max worker
 *   count has been reached.
 * @param reportRuntimeException Determines if the message from runtime exceptions caught during handing of a 
 *   REST request is returned to the caller in addition to the HTTP status code.
 *
 *   Two types of exceptions are raised: `400 Bad Requests` and `500 Internal Server Error`.  If turned on, the
 *   message will be return in the response and the content type set to `text/plain; charset=UTF-8`.
 */
case class RestConfig(
  apiVersion: String,
  rootApiUrl: String,
  swaggerVersion: String = "1.1",
  swaggerApiGroupingPathSegment: Int = 1,
  requestTimeoutSeconds: Int = 60,
  sockoEventCacheTimeoutSeconds: Int = 5,
  maxWorkerCount: Int = 100,
  maxWorkerRescheduleMilliSeconds: Int = 500,
  reportRuntimeException: ReportRuntimeException.Value = ReportRuntimeException.Never) extends Extension {

  val rootApiURI = new URI(rootApiUrl)
  val schemeDomainPort = s"${rootApiURI.getScheme}://${rootApiURI.getHost}" + 
    (if (rootApiURI.getPort > 0) s":${rootApiURI.getPort}" else "") 
  val rootPath = rootApiURI.getPath

  val sockoEventCacheTimeoutMilliSeconds = sockoEventCacheTimeoutSeconds * 1000

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    config.getString(prefix + ".api-version"),
    config.getString(prefix + ".root-api-url"),
    ConfigUtil.getString(config, prefix + ".swagger-version", "1.1"),
    ConfigUtil.getInt(config, prefix + ".swagger-api-grouping-path-segment", 1),
    ConfigUtil.getInt(config, prefix + ".request-timeout-seconds", 60),
    ConfigUtil.getInt(config, prefix + ".socko-event-cache-timeout-seconds", 5),
    ConfigUtil.getInt(config, prefix + ".max-worker-count", 100),
    ConfigUtil.getInt(config, prefix + ".max-worker-reschedule-milliseconds", 500),
    ReportRuntimeException.withName(ConfigUtil.getString(config, prefix + ".report-runtime-exception", "Never")))

  val reportOn400BadRequests = (reportRuntimeException == ReportRuntimeException.BadRequestsOnly ||
    reportRuntimeException == ReportRuntimeException.All)

  val reportOn500InternalServerError = (reportRuntimeException == ReportRuntimeException.InternalServerErrorOnly ||
    reportRuntimeException == ReportRuntimeException.All)
}

/**
 * Indicates if we want to return a runtime exception message to the caller
 *
 * Depending on your security requirements, you may wish to turn off errors in production
 * but turn then on in development.
 *
 * No error messages are returned by default (`Never`).
 */
object ReportRuntimeException extends Enumeration {
  type ReportRuntimeException = Value

  /**
   * Only the HTTP status will be returned
   */
  val Never = Value

  /**
   * The error messages will only be returned in the case of 400 bad requests; i.e.
   * errors from dispatching and deseralizing REST requests.
   */
  val BadRequestsOnly = Value

  /**
   * The error messages will only be returned in the case of 500 internal server error; i.e.
   * errors from processing and seralizing a request
   */
  val InternalServerErrorOnly = Value

  /**
   * The error messages will always be returned for types of exceptions.
   */
  val All = Value

}

