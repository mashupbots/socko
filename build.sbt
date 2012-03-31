
//
// Socko Web Server build file
//


organization := "org.mashupbots"

name := "Socko"

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0"

libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0"

libraryDependencies += "com.typesafe.akka" % "akka-slf4j" % "2.0"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime"

libraryDependencies += "junit" % "junit" % "4.9" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.7.1" % "test"

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource


