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
package org.mashupbots.socko.webserver

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManagerFactory

/**
 * Manages reading key stores and trust stores for TLS/SSL connections 
 *
 * @param server The web server instancing this SSLManager
 */
class SslManager(server: WebServer) {

  private[this] val PROTOCOL = "TLS"

  /**
   * Create context for SSLEngine
   */
  val context: SSLContext = {
    var sslConfig = server.config.ssl.get

    // Set up key manager factory to use our key store (server certificates)
    val ks = KeyStore.getInstance("JKS")
    val is = new FileInputStream(sslConfig.keyStoreFile)
    ks.load(is, sslConfig.keyStorePassword.toCharArray())
    is.close()

    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(ks, sslConfig.keyStorePassword.toCharArray())

    // Setup trust store (client certificates)
    val trustManagers = {
      if (sslConfig.trustStoreFile.isEmpty) {
        null
      } else {
        val ts = KeyStore.getInstance("JKS")
        val tis = new FileInputStream(sslConfig.trustStoreFile.get)
        ts.load(tis, sslConfig.trustStorePassword.get.toCharArray())
        tis.close

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
        trustManagerFactory.init(ts)

        trustManagerFactory.getTrustManagers
      }
    }

    // Initialize the SSLContext to work with our key managers and optional trust store
    val ret = SSLContext.getInstance(PROTOCOL)
    ret.init(kmf.getKeyManagers, trustManagers, null)
    ret
  }

  /**
   * Creates an SSL engine for encoding/decoding SSL traffic
   */
  def createSSLEngine(): SSLEngine = {
      val engine = context.createSSLEngine()
      engine.setNeedClientAuth(server.config.ssl.get.trustStoreFile.isDefined)
      engine.setUseClientMode(false)
      engine
  }

}