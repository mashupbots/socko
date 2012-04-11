# SOCKO

A [Scala](http://www.scala-lang.org/) web server powered by
[Netty](http://netty.io/) networking and [Akka](http://akka.io/) processing.

## Background
We designed MashupBots to be an HTML5 style app. 

Our fat html5/css/javascript client is going to be served as static files from the web server. 
Server side HTML templating will be not required because it will all happen in the browser using javascript.

Our fat client will communicate with our Akka based business logic and database via a HTTP REST API served 
off the same web server.

We could not find a lightweight, asynchronous web server that exactly meets our requirements so we decided 
to write one.

We hope you find it as useful as we do.

## What is Socko?

Socko is:

* Intended for Akka developers
  * who wish to expose their Akka actors as REST endpoints
  * who wish to serve static files and/or HTML 5 applications from the same web server hosting 
    their REST endpoints

* Fast-ish and efficient???
  * TO DO - benchmarking
  * Socko handles and processes incoming HTTP requests in an asynchronous and event driven manner thanks to
    Netty and Akka.
  * Socko also supports blocking IO, for example reading files or database interaction, you can configure Akka
    to use a thread pool instead.

* Lightweight (assuming you are already using Akka)
  * > 1,300 lines of Scala code (and ~2,600 lines of Java code which will be removed once Akka 
    moves to the upcoming Netty 4.0).
  * Socko runs in a standard Scala application. No servlet containers required.
  * Socko has no external dependencies outside Akka 2.0 and Netty (which is a dependency of Akka 2.0 Remote).
  
* Supportive of HTTP Standards
  * HTTP/S and WebSockets
  * HTTP compression
  * HTTP headers
  * Decoding HTTP POST, file uploads and query strings

* Trying to be as Simple and as Easy as possible
  * Routing DSL like [Unfilted HTTP ToolKit](http://unfiltered.databinder.net/Unfiltered.html) and 
    [Play Mini](https://github.com/typesafehub/play2-mini)
  * Configurable from inside your code and/or via settings in Akka's configuration file.
  * Plenty of [examples](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples)


## What Socko is NOT

* Socko is not a servlet. We use Netty instead.
  
* Socko is not a web application or MVC framework like Lift or Play. It does not perform server side
  HTML templating. We use client side javascript libraries like EmberJS and BackboneJS instead.
    
* Socko does not store session data or cache application data. We use HTML5 local storage instead.


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
    case ctx @ GET(_) => {
      actorSystem.actorOf(Props[HelloProcessor]) ! ctx
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


## Other Info

* [Issues] (https://github.com/mashupbots/socko/issues?milestone=&sort=created&direction=desc&state=open)
* [Road Map](https://github.com/mashupbots/socko/issues/milestones)



  
