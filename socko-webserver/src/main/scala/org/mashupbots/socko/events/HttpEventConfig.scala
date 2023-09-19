//
// Copyright 2012 Vibul Imtarnasan, David Bolton and Socko contributors.
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
package org.mashupbots.socko.events

import org.apache.pekko.actor.ActorRef

/**
 * HTTP event configuration used in the processing of [[org.mashupbots.socko.events.HttpEvent]]s.
 *
 * @param serverName Name of this instance of the Socko Web Server
 * @param minCompressibleContentSizeInBytes Minimum number of bytes before content will be compressed if requested by
 *   the client. Set to `-1` to turn off compression.
 * @param maxCompressibleContentSizeInBytes Maximum number of bytes before HTTP content will be not be compressed if
 *   requested by the client. Defaults to 1MB otherwise too much CPU maybe taken up for compression.
 * @param compressibleContentTypes List of MIME types of that can be compressed.
 * @param webLogWriter Actor to which web log events to be sent
 */
case class HttpEventConfig(
  serverName: String,
  minCompressibleContentSizeInBytes: Int,
  maxCompressibleContentSizeInBytes: Int,
  compressibleContentTypes: List[String],
  webLogWriter: Option[ActorRef]) {

}