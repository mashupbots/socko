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
  version      := "0.8.0",
  crossScalaVersions := Seq("2.12.17", "2.13.10"),

  // Repositories
  resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
  resolvers += "Asana Maven" at "s3://asana-oss-cache/maven/release",
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  
  Test / fork := true,
)

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % scalaVersion.value,
)

// Compile settings
lazy val compileJdkSettings = Seq(
  scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-opt:l:inline", "-opt-inline-from:**", "-feature", "-language:postfixOps"),
  javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
)
  
//
// Projects
//
lazy val root = (project in file("."))
  .settings(shared ++ compileJdkSettings ++ Seq(
    publish / skip := true
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
      publishTo := Some("Asana Maven" at "s3://asana-oss-cache/maven/release"),
      Test / publishArtifact := false,
    )
  )




