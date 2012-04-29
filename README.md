# SOCKO

An embedded [Scala](http://www.scala-lang.org/) web server powered by
[Netty](http://netty.io/) networking and [Akka](http://akka.io/) processing.

## Background
We are currently working on a HTML5 style app called MashupBots.

We wanted an open source, lightweight and embeddable Scala web server that can serve static files and support RESTful APIs to our business logic implemented in Akka.

We do not need a web application framework: server side templating, caching, session management, etc.

We couldn't find a web server that exactly meets our requirements, so we decided to write one.

We hope you find it as useful as we do.

## What is Socko?

Socko is:

* Intended for Akka developers
  * who wish to expose their Akka actors as REST endpoints
  * who wish to serve static files and/or HTML 5 applications from the same web server hosting 
    their REST endpoints

* Embedded
  * Socko runs within your Scala application. 
  * Routing DSL like [Unfilted](http://unfiltered.databinder.net/Unfiltered.html) and 
    [Play2 Mini](https://github.com/typesafehub/play2-mini). Route by HTTP method, host, path and querystring.
    See [example](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/routes/RouteApp.scala).
  * Configurable from inside your code and/or via settings in Akka's configuration file.

* Lightweight (assuming you are already using Akka)
  * ~1,300 lines of Scala code (and ~2,600 lines of Java code which will be removed once Akka 
    moves to the upcoming Netty 4.0).
  * Socko has no external dependencies outside Akka 2.0 Remote (which includes Netty).

* Fast-ish
  * Socko handles and processes incoming HTTP requests in an asynchronous and event driven manner thanks to
    Netty and Akka.
  * Initial benchmarking [results](http://sockoweb.org/2012/04/22/benchmark.html)

* Supportive of HTTP and HTML5 Standards
  * HTTP/S and WebSockets
  * HTTP compression
  * HTTP headers
  * Decoding HTTP POST, file uploads and query strings
  
* Open Source
  * [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0)


## What Socko is NOT

* Socko is not a standalone web server.  It must be embedded within your application.

* Socko is not a servlet container. It will not run your servlet or JSP pages.
  
* Socko is not a web application or MVC framework like Lift or Play. It does not perform server side
  HTML templating. We use client side javascript libraries like EmberJS and BackboneJS instead.


## Quick Start

```scala
object HelloApp extends Logger {

  //
  // STEP #1 - Define actors and start Akka
  // See `HelloProcessor` below
  //
  val actorSystem = ActorSystem("HelloExampleActorSystem")
  
  //
  // STEP #2 - Define routes. 
  //
  val routes = Routes({
    case GET(httpRequest) => {
      actorSystem.actorOf(Props[HelloProcessor]) ! httpRequest
    }
  })

  //
  // STEP #3 - Start and Stop web server.
  //
  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes)
    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    System.out.println("Open your browser and navigate to http://localhost:8888"); 
  }
}

class HelloProcessor extends Actor {
  def receive = {
    case request: HttpRequestProcessingContext =>
      request.writeResponse("Hello from Socko (" + new Date().toString + ")")
      context.stop(self)
  }
}
```


## Developer Information

### Getting the Source

Get the source code from github

    $ git clone git@github.com:mashupbots/socko.git
    $ cd socko

### Run Unit Tests

    $ sbt test

### Run Examples

    $ sbt 
    > project socko-examples
    > run

### Editing the Source Code

* We are currently using [Eclipse 3.7 Indigo](http://www.eclipse.org/downloads/packages/eclipse-ide-javascript-web-developers/indigosr2) 
  with [Scala IDE nightly build](http://scala-ide.org/download/nightly.html)

* Generate eclipse project files: `sbt eclispse`

* Start `Eclipse`

* From the top memu, select `File` | `Import`
  * Select `General` | `Existing Projects into Workspace`. 
  * Click `Next`.
  * Select the `socko` source code directory as the root
  * Should see `socko-examples` and `socko-webserver` under `Projects`
  * Click `Finish`

* To run the scalatest unit test cases, just right click on a test class file and select `Run As JUnit Test`.


## Links

* [Web Site](http://sockoweb.org/)
* [Issues] (https://github.com/mashupbots/socko/issues?milestone=&sort=created&direction=desc&state=open)
* [Road Map](https://github.com/mashupbots/socko/issues/milestones)
* [Examples](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples)



  
