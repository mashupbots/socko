//
// Socko Web Server build file
//

import sbt._
import Keys._

//
// Build setup
//
object SockoBuild extends Build {

  lazy val root = Project(id = "socko",
                          base = file(".")) aggregate(webserver, examples)

  lazy val webserver = Project(id = "socko-webserver",
                         base = file("socko-webserver"),
                         settings = defaultSettings ++ Seq(
                           libraryDependencies ++= Dependencies.webserver
                         ))

  lazy val examples = Project(id = "socko-examples",
                         base = file("socko-examples"),
                         dependencies = Seq(webserver))
                          
  lazy val defaultSettings = Defaults.defaultSettings  ++ Seq(
    // Repositories
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",

    // Compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked") ++ (
      if (true || (System getProperty "java.runtime.version" startsWith "1.7")) Seq() else Seq("-optimize")), // -optimize fails with jdk7
    javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
  )
  
}

//
// Dependencies
//
object Dependencies {
  import Dependency._

  val webserver = Seq(
    Dependency.akkaActor, Dependency.akkaRemote, Dependency.akkaSlf4j, Dependency.akkaTestKit,
    Dependency.logback, Dependency.junit, Dependency.scalatest
  )
}

object Dependency {
  val akkaActor     = "com.typesafe.akka"           % "akka-actor"             % "2.0"
  val akkaRemote    = "com.typesafe.akka"           % "akka-remote"            % "2.0"
  val akkaSlf4j     = "com.typesafe.akka"           % "akka-slf4j"             % "2.0"
  val akkaTestKit   = "com.typesafe.akka"           % "akka-testkit"           % "2.0"
  val logback       = "ch.qos.logback"              % "logback-classic"        % "1.0.0"
  val junit         = "junit"                       % "junit"                  % "4.9"     % "test"
  val scalatest     = "org.scalatest"               %% "scalatest"             % "1.7.1"   % "test"
}

