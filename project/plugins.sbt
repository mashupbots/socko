
resolvers += Resolver.url("artifactory", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

// https://github.com/typesafehub/sbteclipse
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.3.0")

// https://github.com/sbt/sbt-pgp
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

// https://github.com/sbt/sbt-assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.2")

