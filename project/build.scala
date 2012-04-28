//
// Socko Web Server build file
//

import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import sbt.Project.Initialize

//
// Build setup
//
object SockoBuild extends Build {

  //
  // Settings
  //
  lazy val defaultSettings = Defaults.defaultSettings ++ Seq(
    // Info
    organization := "org.mashupbots.socko",
    version      := "0.1.0-SNAPSHOT",

    // Repositories
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    
    // Compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked") ++ (
      if (true || (System getProperty "java.runtime.version" startsWith "1.7")) Seq() else Seq("-optimize")), // -optimize fails with jdk7
    javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    
    // sbtEclipse - see examples https://github.com/typesafehub/sbteclipse/blob/master/sbteclipse-plugin/src/sbt-test/sbteclipse/02-contents/project/Build.scala
    EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.Unmanaged, EclipseCreateSrc.Source, EclipseCreateSrc.Resource),
    EclipseKeys.withSource := true    
  )
    
  //
  // Packaging to SonaType using SBT
  //
  // https://github.com/sbt/sbt.github.com/blob/gen-master/src/jekyll/using_sonatype.md
  // http://www.cakesolutions.net/teamblogs/2012/01/28/publishing-sbt-projects-to-nexus/
  // https://docs.sonatype.org/display/Repository/How+To+Generate+PGP+Signatures+With+Maven
  //    
  def sockoPomExtra = {
    <url>http://www.sockoweb.org</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:mashupbots/socko.git</url>
      <connection>scm:git:git@github.com:mashupbots/socko.git</connection>
    </scm>
    <developers>
      <developer>
        <id>veebs</id>
        <name>Vibul Imtarnasan</name>
        <url>https://github.com/veebs</url>
      </developer>
      <developer>
        <id>lightningdb</id>
        <name>David Bolton</name>
        <url>https://github.com/lightningdb</url>
      </developer>
    </developers>
  }

  def sockoPublishTo: Initialize[Option[Resolver]] = {
    (version) { version: String =>
      val nexus = " https://oss.sonatype.org/"
      if (version.trim.endsWith("SNAPSHOT")) {
        Some("snapshots" at nexus + "content/repositories/snapshots/")
      } else {
        Some("releases" at nexus + "service/local/staging/deploy/maven2/")
      }
    }
  }
    
  //
  // Projects
  //
  lazy val root = Project(id = "socko",
                          base = file("."),
                          settings = defaultSettings) aggregate(webserver, examples)

  lazy val webserver = Project(id = "socko-webserver",
                         base = file("socko-webserver"),
                         settings = defaultSettings ++ Seq(
                           libraryDependencies ++= Dependencies.webserver,
                           publishTo <<= sockoPublishTo,
                           publishMavenStyle := true,
                           publishArtifact in Test := false,
                           pomIncludeRepository := { x => false },
                           pomExtra := sockoPomExtra
                         ))

  lazy val examples = Project(id = "socko-examples",
                         base = file("socko-examples"),
                         dependencies = Seq(webserver),
                         settings = defaultSettings ++ Seq(
                           libraryDependencies ++= Dependencies.examples
                         ))  
}

//
// Dependencies
//
object Dependencies {
  import Dependency._

  val webserver = Seq(
    Dependency.akkaActor, Dependency.netty, Dependency.akkaSlf4j, Dependency.akkaTestKit,
    Dependency.logback, Dependency.junit, Dependency.scalatest
  )
  
  val examples = Seq(
    Dependency.logback
  )  
}

object Dependency {
  val akkaActor     = "com.typesafe.akka"           % "akka-actor"             % "2.0"
  val akkaSlf4j     = "com.typesafe.akka"           % "akka-slf4j"             % "2.0"
  val akkaTestKit   = "com.typesafe.akka"           % "akka-testkit"           % "2.0"
  val junit         = "junit"                       % "junit"                  % "4.9"     % "test"
  val logback       = "ch.qos.logback"              % "logback-classic"        % "1.0.0"   % "runtime"
  val netty         = "io.netty"                    % "netty"                  % "3.3.0.Final"
  val scalatest     = "org.scalatest"               %% "scalatest"             % "1.7.1"   % "test"
}




