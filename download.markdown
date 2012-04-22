---
layout: article
---
#Download Socko

## Released Version

Coming soon ...


## Build from Source

### 1. Get the source

Download the source code from GitHub

    $ git clone git@github.com:mashupbots/socko.git
    $ cd socko

### 2. Prerequisites

Please install the following:
 - [Java 6](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
 - [Scala 2.9](http://www.scala-lang.org/) or higher
 - [Simple Build Tool](https://github.com/harrah/xsbt/wiki/Getting-Started-Setup)


### 3. Build

Run Simpble Build Tool to compile and test

    $ sbt
    $ compile
    $ test
    $ package

The resultant .jar file can be found in `socko-webserver/target/scala-2.9.1/socko-webserver_2.9.1-X.X.X.jar`.

### 4. Run Examples

    $ project socko-examples
    $ run

