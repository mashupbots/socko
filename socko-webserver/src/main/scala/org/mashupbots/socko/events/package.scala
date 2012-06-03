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
 * Socko events are raised by Socko and passed into your routes for dispatching to your handlers.
 * Socko events provides a bridge between Netty and Akka.
 * 
 * There are 4 types of [[org.mashupbots.socko.events.SockoEvent]]
 *  - [[org.mashupbots.socko.events.HttpRequestEvent]] - Fired when a HTTP request is received
 *  - [[org.mashupbots.socko.events.HttpChunkEvent]] - Fired when a HTTP chunk is received. Typically only
 *    used with large request data such as file upload.
 *  - [[org.mashupbots.socko.events.WebSocketHandshakeEvent]] - Fired prior to a web socket handshake to establish
 *    a web socket connection. 
 *  - [[org.mashupbots.socko.events.WebSocketFrameEvent]] - Fired when a WebSocket text or binary frame is received.
 */
package org.mashupbots.socko.events {
}