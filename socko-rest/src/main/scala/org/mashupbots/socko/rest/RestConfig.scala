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

/**
 * Configuration for REST handler
 *
 * @param apiVersion the version of your API
 * @param rootUrl Root url path to your API from an external caller's point of view
 * @param swaggerVersion Swagger definition version
 * @param requestTimeoutSeconds Number of seconds before a request is timed out
 * @param maxWorkerCount Maximum number of workers per [[org.mashupbots.socko.rest.RestHandler]].
 * @param maxWorkerRescheduleMilliSeconds Reschedule a message for processing again using this delay when max worker
 *   count has been reached.
 */
case class RestConfig(
  apiVersion: String,
  rootUrl: String,
  swaggerVersion: String = "1.1",
  requestTimeoutSeconds: Int = 60,
  maxWorkerCount: Int = 100,
  maxWorkerRescheduleMilliSeconds: Int = 500) extends Extension {

  /**
   * Read configuration from AKKA's `application.conf`
   */
  def this(config: Config, prefix: String) = this(
    config.getString(prefix + ".api-version"),
    config.getString(prefix + ".root-url"),
    ConfigUtil.getString(config, prefix + ".swagger-version", "1.1"),
    ConfigUtil.getInt(config, prefix + ".request-timeout-seconds", 60),
    ConfigUtil.getInt(config, prefix + ".max-worker-count", 100),
    ConfigUtil.getInt(config, prefix + ".max-worker-reschedule-milliseconds", 500))
}