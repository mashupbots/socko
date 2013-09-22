---
layout: docs
title: Socko User Guide - Build Tool

BuilderClass: <code><a href="../api/#org.mashupbots.socko.buildtools.Builder">Builder</a></code>
---
# Socko User Guide - Build Tool

## Introduction

When developing HTML javascript applications, it is quite common to have a build file that concatenates/compresses 
javascript, CSS and HTML files among other tasks.

During development, if you change a file, you have to run the build before clicking refresh in your browser in order to see and 
test your changes.

Manually running the build every time you make a change will challenge your sanity and can lead to premature 
[RSI](http://en.wikipedia.org/wiki/Repetitive_strain_injury).

This is where Socko build tools can help.

While you are coding, Socko's {{ page.BuilderClass }} is able to watch for changes in your source files and automatically run your
build for you.  The result of your build is logged to the console.




## Adding the Build Tool Module to Your Project

In order to use the Socko Javascript Build Tool, you need to add an extra dependency.

Add the following to your `build.sbt`.  Replace `X.Y.Z` with the version number.

    libraryDependencies += "org.mashupbots.socko" %% "socko-buildtools" % "X.Y.Z"

Make sure you are using **JDK 7**.




## Usage <a class="blank" id="How">&nbsp;</a>

In your application, add the following to start and stop the builder:

{% highlight scala %}
    // Start builder
    val builder = new Builder("build command line", "/path/to/src/directory")

    // Start Socko web server, etc...
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    ...

    // Stop builder
    builder.stop()
{% endhighlight %}

When {{ page.BuilderClass }} is instanced, it starts a new thread to watch the directory `/path/to/src/directory`.  When there
is a change in the directory and/or sub-directories, `build command line` is executed.

Examples of possible `build command line` values:

{% highlight scala %}
    // Ant
    val builder = new Builder("ant -f /home/username/dev/project/build.xml", "path/to/src/directory")

    // Run Ant in this JVM rather than in a new external process
    val builder = new Builder("internal-ant /home/username/dev/project/build.xml dist", "path/to/src/directory")

    // Run shell script or bat file
    val builder = new Builder("/home/username/dev/project/build.sh", "path/to/src/directory")
{% endhighlight %}

See {{ page.BuilderClass }} for a complete list of configuration options.

See the [code example](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/builder)
for a worked example.


