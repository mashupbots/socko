

import sbt._

import Dependencies._
                       

//
// Dependencies
//
object Dependencies {
  import Dependency._

  val webserver = Seq(
    Dependency.pekkoActor, Dependency.pekkoSlf4j, Dependency.pekkoTestKit,
    Dependency.netty, Dependency.concurrentmap, Dependency.nextProtoNeg,
    Dependency.logback, Dependency.scalatest
  )
  
  val buildtools = Seq(
    Dependency.ant, Dependency.logback, Dependency.scalatest
  )  

  val rest = Seq(
    Dependency.json4s, Dependency.logback, 
    Dependency.scalatest, Dependency.pekkoTestKit
  )  

  val examples = Seq(
    Dependency.logback
  )  
}

object Dependency {
  object V {
    val Pekko        = "1.0.0"
  }
  val pekkoActor     = "org.apache.pekko"                       %% "pekko-actor"                  % V.Pekko
  val pekkoSlf4j     = "org.apache.pekko"                       %% "pekko-slf4j"                  % V.Pekko
  val pekkoTestKit   = "org.apache.pekko"                       %% "pekko-testkit"                % V.Pekko % "test"
  val ant           = "org.apache.ant"                          % "ant"                          % "1.8.4"
  val concurrentmap = "com.googlecode.concurrentlinkedhashmap"  % "concurrentlinkedhashmap-lru"  % "1.3.2"
  val logback       = "ch.qos.logback"                          % "logback-classic"              % "1.2.3" % "runtime"
  val netty         = "io.netty"                                % "netty-all"                    % "4.1.52.Final"
  val nextProtoNeg  = "org.eclipse.jetty.npn"                   % "npn-api"                      % "1.1.0.v20120525"
  val json4s        = "org.json4s"                              %% "json4s-native"               % "3.4.2"
  val scalatest     = "org.scalatest"                           %% "scalatest"                   % "3.0.8" % "test"
}
