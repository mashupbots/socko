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
package org.mashupbots.socko.examples.secure

import java.io.File

import org.mashupbots.socko.routes._
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.webserver.SslConfig
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props

/**
 * This is a clone of the quick start `HelloApp` ... but using HTTPS.
 * 
 * Before you run this example, you will have to create a keystore file using `keytool`. Open a terminal window and 
 * enter the following commands:
 * {{{
 *   keytool -genkey -keystore /tmp/myKeyStore -keyalg RSA
 *   Enter keystore password: password
 *   What is your first and last name? [press ENTER]
 *   What is the name of your organizational unit? [press ENTER]
 *   What is the name of your organization? [press ENTER]
 *   What is the name of your State or Province? [press ENTER]
 *   Is CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown correct? yes
 *   Enter key password for <mykey> [press ENTER]
 * }}}
 * 
 * You can now run this example:
 *  - Run this class as a Scala Application
 *  - Open your browser and navigate to `https://localhost:8888/`
 */      
object SecureApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  // See `HelloHandler`
  //
  val actorSystem = ActorSystem("SecureExampleActorSystem")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case GET(request) => {
      actorSystem.actorOf(Props[SecureHelloHandler]) ! request
    }
  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    val keyStoreFile = new File("/tmp/myKeyStore")
    val keyStoreFilePassword = "password"

    if (!keyStoreFile.exists) {
      System.out.println("Cannot find keystore file: " + keyStoreFile.getAbsolutePath)
      System.out.println("")
      System.out.println("Please create the file using the command:")
      System.out.println("  keytool -genkey -keystore " + keyStoreFile.getAbsolutePath + " -keyalg RSA")
      System.out.println("    Enter keystore password: " + keyStoreFilePassword)
      System.out.println("    What is your first and last name? [press ENTER]")
      System.out.println("    What is the name of your organizational unit? [press ENTER]")
      System.out.println("    What is the name of your organization? [press ENTER]")
      System.out.println("    What is the name of your State or Province? [press ENTER]")
      System.out.println("    What is the two-letter country code for this unit? [press ENTER]")
      System.out.println("    Is CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown correct? yes")
      System.out.println("    Enter key password for <mykey> [press ENTER]")
      System.out.println("")
      System.out.println("Web Server terminated")
      return
    }

    val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None)
    val webServer = new WebServer(WebServerConfig(ssl = Some(sslConfig)), routes, actorSystem)
    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    System.out.println("Open your browser and navigate to https://localhost:8888")
    System.out.println("Because this is a self-signed certificate, you will see a warning form the browser: " +
        "The site's security certificate is not trusted!")
    System.out.println("Trust this certificate and proceed.")
    
  }

}