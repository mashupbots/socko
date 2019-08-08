

import sbt._

import Dependencies._
                       

//
// Dependencies
//
object Dependencies {
  import Dependency._

  val webserver = Seq(
    Dependency.akkaActor, Dependency.akkaSlf4j, Dependency.akkaTestKit,
    Dependency.netty, Dependency.concurrentmap, Dependency.nextProtoNeg,
    Dependency.logback, Dependency.scalatest
  )
  
  val buildtools = Seq(
    Dependency.ant, Dependency.logback, Dependency.scalatest
  )  

  val rest = Seq(
    Dependency.json4s, Dependency.logback, 
    Dependency.scalatest, Dependency.akkaTestKit
  )  

  val examples = Seq(
    Dependency.logback
  )  
}

object Dependency {
  object V {
    val Akka        = "2.5.11"
  }
  val akkaActor     = "com.typesafe.akka"                       %% "akka-actor"                  % V.Akka
  val akkaSlf4j     = "com.typesafe.akka"                       %% "akka-slf4j"                  % V.Akka
  val akkaTestKit   = "com.typesafe.akka"                       %% "akka-testkit"                % V.Akka % "test"
  val ant           = "org.apache.ant"                          % "ant"                          % "1.8.4"
  val concurrentmap = "com.googlecode.concurrentlinkedhashmap"  % "concurrentlinkedhashmap-lru"  % "1.3.2"
  val logback       = "ch.qos.logback"                          % "logback-classic"              % "1.0.9" % "runtime"
  val netty         = "io.netty"                                % "netty-all"                    % "4.1.19.Final"
  val nextProtoNeg  = "org.eclipse.jetty.npn"                   % "npn-api"                      % "1.1.0.v20120525"
  val json4s        = "org.json4s"                              %% "json4s-native"               % "3.4.2"
  val scalatest     = "org.scalatest"                           %% "scalatest"               % "3.0.8" % "test"
}
