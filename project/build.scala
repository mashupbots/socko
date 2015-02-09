//
// Socko Web Server build file
//

import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import sbt.Project.Initialize
import sbtassembly.Plugin._
import AssemblyKeys._

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
    version      := "0.6.0",
    scalaVersion := Dependency.V.Scala,
    organizationHomepage := Some(url("http://www.sockoweb.org")),

    // Repositories
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
        
    // sbtEclipse - see examples https://github.com/typesafehub/sbteclipse/blob/master/sbteclipse-plugin/src/sbt-test/sbteclipse/02-contents/project/Build.scala
    EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.Unmanaged, EclipseCreateSrc.Source, EclipseCreateSrc.Resource),
    EclipseKeys.withSource := true,
    
    fork in Test := true,

    // Publishing details - see http://www.scala-sbt.org/release/docs/Community/Using-Sonatype.html
    publishTo <<= sockoPublishTo,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    pomExtra := sockoPomExtra
  )
  
  // Compile settings
  lazy val compileJdk6Settings = Seq(
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-optimize", "-feature", "-language:postfixOps", "-target:jvm-1.6"),
    javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.6", "-target", "1.6")
  )
  lazy val compileJdk7Settings = Seq(
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-optimize", "-feature", "-language:postfixOps", "-target:jvm-1.7"),
    javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.7", "-target", "1.7")
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
      val nexus = "https://oss.sonatype.org/"
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
                          settings = defaultSettings ++ Unidoc.settings ++ Seq(
                            Unidoc.unidocExclude := Seq(examples.id),
                            publishArtifact := false
                          )) aggregate(webserver, buildtools, rest, examples)

  lazy val webserver = Project(id = "socko-webserver",
                         base = file("socko-webserver"),
                         settings = defaultSettings ++ compileJdk6Settings ++ Seq(
                           libraryDependencies ++= Dependencies.webserver
                         ))
                         
  lazy val buildtools = Project(id = "socko-buildtools",
                         base = file("socko-buildtools"),
                         dependencies = Seq(webserver),
                         settings = defaultSettings ++ compileJdk7Settings ++ Seq(
                           libraryDependencies ++= Dependencies.buildtools
                         ))  

  lazy val rest = Project(id = "socko-rest",
                         base = file("socko-rest"),
                         dependencies = Seq(webserver),                         
                         settings = defaultSettings ++ compileJdk6Settings ++ Seq(
                           libraryDependencies ++= Dependencies.rest
                         ))  

  lazy val examples = Project(id = "socko-examples",
                         base = file("socko-examples"),
                         dependencies = Seq(webserver, rest, buildtools),
                         settings = defaultSettings ++ compileJdk7Settings ++ Seq(
                           libraryDependencies ++= Dependencies.examples,
                           publishArtifact := false
                         ))  
}

//
// Dependencies
//
object Dependencies {
  import Dependency._

  val webserver = Seq(
    Dependency.scalaReflect,
    Dependency.akkaActor, Dependency.akkaSlf4j, Dependency.akkaTestKit,
    Dependency.netty, Dependency.concurrentmap, Dependency.nextProtoNeg,
    Dependency.logback, Dependency.scalatest
  )
  
  val buildtools = Seq(
    Dependency.ant, Dependency.logback, Dependency.scalatest
  )  

  val rest = Seq(
    Dependency.json4s, Dependency.json4sExt, Dependency.logback, 
    Dependency.scalatest, Dependency.akkaTestKit
  )  

  val examples = Seq(
    Dependency.logback
  )  
}

object Dependency {
  object V {
    val Scala       = "2.11.2"
    val Akka        = "2.3.6"
  }

  val scalaReflect  = "org.scala-lang"                          % "scala-reflect"                % V.Scala
  val akkaActor     = "com.typesafe.akka"                       %% "akka-actor"                  % V.Akka
  val akkaSlf4j     = "com.typesafe.akka"                       %% "akka-slf4j"                  % V.Akka
  val akkaTestKit   = "com.typesafe.akka"                       %% "akka-testkit"                % V.Akka % "test"
  val ant           = "org.apache.ant"                          % "ant"                          % "1.8.4"
  val concurrentmap = "com.googlecode.concurrentlinkedhashmap"  % "concurrentlinkedhashmap-lru"  % "1.3.2"
  val logback       = "ch.qos.logback"                          % "logback-classic"              % "1.0.9" % "runtime"
  val netty         = "io.netty"                                % "netty-all"                    % "4.0.23.Final"
  val nextProtoNeg  = "org.eclipse.jetty.npn"                   % "npn-api"                      % "1.1.0.v20120525"
  val json4s        = "org.json4s"                              %% "json4s-native"               % "3.2.11"
  val json4sExt        = "org.json4s"                              %% "json4s-ext"               % "3.2.11"
  val scalatest     = "org.scalatest"                           % "scalatest_2.11"               % "2.2.1" % "test"
}




