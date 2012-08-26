
resolvers += Resolver.url("artifactory", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

// https://github.com/typesafehub/sbteclipse
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0")

// https://github.com/sbt/xsbt-gpg-plugin
addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

// https://github.com/sbt/sbt-assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.4")

