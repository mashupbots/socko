---
layout: article
---
#Download Socko

## Latest Release

The latest verison of Socko is **`0.4.1`** released on 9th January 2014.

Download [socko-webserver_2.10-0.4.1.jar](https://oss.sonatype.org/content/groups/public/org/mashupbots/socko/socko-webserver_2.10/0.4.1/socko-webserver_2.10-0.3.1.jar).

[Change logs](https://github.com/mashupbots/socko/issues/milestones?state=closed) and 
[road maps](https://github.com/mashupbots/socko/issues/milestones?state=open) are available on our issue tracker.

If you have a question or need help, just create a ticket in our [issue register](https://github.com/mashupbots/socko/issues).

## Simple Build Tool 

If you are using Scala 2.10, add the following to your `build.sbt`.  Replace `X.Y.Z` with the
version number.

    libraryDependencies += "org.mashupbots.socko" %% "socko-webserver" % "X.Y.Z"

If you are not using the above scala version(s), use the following instead
   
    libraryDependencies += "org.mashupbots.socko" % "socko-webserver_2.10" % "X.Y.Z"


## Build from Source

### 1. Get the source

Download the source code from GitHub

    $ git clone git@github.com:mashupbots/socko.git
    $ cd socko

### 2. Prerequisites

Please install the following:
 - [Java JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
 - [Scala 2.10.2](http://www.scala-lang.org/) or higher
 - [Simple Build Tool](https://github.com/harrah/xsbt/wiki/Getting-Started-Setup)


### 3. Build

Run Simple Build Tool to compile and test

    $ sbt
    $ compile
    $ test
    $ package

The resultant JAR files can be found in:
 - `socko-webserver/target/scala-2.10/socko-webserver_2.10-X.X.X.jar`.
 - `socko-rest/target/scala-2.10/socko-rest_2.10-X.X.X.jar`.
 - `socko-buildtools/target/scala-2.10/socko-buildtools_2.10-X.X.X.jar`.

### 4. Run Examples

    $ project socko-examples
    $ run

