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
 * Socko example applications.
 * 
 *  ==[[org.mashupbots.socko.quickstart.HelloApp]]==
 *  Quick start application that illustrates our 3 steps to staring your web server.
 *  
 *  ==[[org.mashupbots.socko.config.AkkaConfigApp]]==
 *  Illustrates how to load web server configuration from an Akka `application.conf` file.
 *  
 *  ==[[org.mashupbots.socko.config.CodeConfigApp]]==
 *  Illustrates how to programmatically set web server configuration.
 *  
 *  ==[[org.mashupbots.socko.routes.RouteApp]]==
 *  Illustrates how to route (dispatch) requests to your Akka actors using HTTP method, host, 
 *  path and querystring.
 *
 *  ==[[org.mashupbots.socko.secure.SecureApp]]==
 *  Illustrates how to create and use a self-signed certificate so that HTTPS can be used from the browser.  
 *  
 *  ==[[org.mashupbots.socko.snoop.SnoopApp]]==
 *  Illustrates how to use our [[org.mashupbots.socko.processors.SnoopProcessor]] to assist with debugging 
 *  incoming web requests.
 *  
 *  ==[[org.mashupbots.socko.websocket.WebSocketApp]]==
 *  Illustrates how to setup web sockets.
 *  
 *  
 */
package object examples {
}