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

import org.mashupbots.socko.infrastructure.Logger
import org.scalatest.Finders
import org.scalatest.GivenWhenThen
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider

class RestConfigSpec extends WordSpec with MustMatchers with GivenWhenThen with Logger {

  "RestConfig" must {

    "correctly load defaults" in {
      val cfg = RestConfig(apiVersion = "1", rootPath = "/api")

      cfg.apiVersion must be("1")
      cfg.rootPath must be("/api")
      cfg.swaggerVersion must be("1.1")
      cfg.swaggerApiGroupingPathSegment must be(1)
      cfg.requestTimeoutSeconds must be(60)
      cfg.maxWorkerCount must be(100)
      cfg.maxWorkerRescheduleMilliSeconds must be(500)
      cfg.reportRuntimeException must be(ReportRuntimeException.Never)
    }

    "correctly load from akka config" in {
      val actorConfig = """
		my-rest-config {
		  api-version = "1"
		  root-path = "/api"
		  swagger-version = "2"
          swagger-api-grouping-path-segment = 3 
          request-timeout-seconds= 4
          max-worker-count = 5
          max-worker-reschedule-milliseconds = 6
          report-runtime-exception = "All"
		}"""

      val actorSystem = ActorSystem("RestConfigSpec", ConfigFactory.parseString(actorConfig))
      val cfg = MyRestConfig(actorSystem)

      cfg.apiVersion must be("1")
      cfg.rootPath must be("/api")
      cfg.swaggerVersion must be("2")
      cfg.swaggerApiGroupingPathSegment must be(3)
      cfg.requestTimeoutSeconds must be(4)
      cfg.maxWorkerCount must be(5)
      cfg.maxWorkerRescheduleMilliSeconds must be(6)
      cfg.reportRuntimeException must be(ReportRuntimeException.All)
    }

  }
}

object MyRestConfig extends ExtensionId[RestConfig] with ExtensionIdProvider {
  override def lookup = MyRestConfig
  override def createExtension(system: ExtendedActorSystem) =
    new RestConfig(system.settings.config, "my-rest-config")
}