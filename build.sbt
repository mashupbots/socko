//
// Socko Web Server build file
//

// import sbt.Project.Initialize
// import sbtassembly.Plugin._
// import AssemblyKeys._

//
// Build setup
//

//
// Settings
//
val shared = Seq(
  // Info
  organization := "com.github.asana.socko-asana-fork",
  version      := "0.6.1",
  crossScalaVersions := Seq("2.11.12", "2.12.8"),

  // Repositories
  resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  
  Test / fork := true,
)

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % scalaVersion.value,
)

// Compile settings
lazy val compileJdkSettings = Seq(
  scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-optimize", "-feature", "-language:postfixOps"),
  javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.7", "-target", "1.7")
)
  
//
// Projects
//
lazy val root = (project in file("."))
  .settings(shared ++ compileJdkSettings ++ Seq(
    skip in publish := true
    )
  ).aggregate(webserver)

lazy val webserver = (project in file("socko-webserver"))
  .settings(
    shared ++ compileJdkSettings ++ Seq(
      libraryDependencies ++= Dependencies.webserver ++ Seq(
       "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      ),
      licenses := List(
          ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
        ),
      homepage := Some(url("https://github.com/Asana/socko-asana-fork")),
      publishMavenStyle := true,
      publishArtifact in Test := false,
      bintrayOrganization := Some("asana"),
      bintrayRepository := "maven",
      bintrayPackage := "socko-asana-fork",
      bintrayReleaseOnPublish in ThisBuild := true,
    )
  )




