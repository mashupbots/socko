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
package org.mashupbots.socko

/**
 * Context objects provides a uniform way to read request data and write response data.
 * 
 * Socko uses [[org.mashupbots.socko.context.ProcessingContext]] as the bridge between Netty handlers and Akka actor
 * processors. [[org.mashupbots.socko.context.ProcessingContext]] are created by Socko's Netty request handler
 * ([[org.mashupbots.socko.webserver.RequestHandler]]) and passed to your Akka actor processors via routes.
 * 
 * There are 4 types of [[org.mashupbots.socko.context.ProcessingContext]]
 *  - [[org.mashupbots.socko.context.HttpRequestProcessingContext]] - Context for processing a standard HTTP request.
 *  - [[org.mashupbots.socko.context.HttpChunkProcessingContext]] - Context for processing a HTTP chunk. Typically only
 *    used with large request data such as file upload.
 *  - [[org.mashupbots.socko.context.WsHandshakeProcessingContext]] - Context for performing WebSocket handshaking. 
 *  - [[org.mashupbots.socko.context.WsProcessingContext]] - Context for processing a WebSocket text or binary frame.
 */
package object context {
}