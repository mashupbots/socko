---
layout: docs
title: Socko User Guide - Quick Start

SockoEventClass: <code><a href="../api/#org.mashupbots.socko.events.SockoEvent">SockoEvent</a></code>
HttpRequestEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpRequestEvent">HttpRequestEvent</a></code>
WebServerClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServer">WebServer</a></code>
WebServerConfigClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServerConfig">WebServerConfig</a></code>
---
# Socko User Guide - Quick Start


## Get Socko

You must have **Scala 2.10** installed. We recommend that you have **JDK 7** installed.

Add the following to your `build.sbt`.  Replace `X.Y.Z` with the version number.

    libraryDependencies += "org.mashupbots.socko" %% "socko-webserver" % "X.Y.Z"


Note: JDK 6 is supported for the `webserver` and `rest` modules. All other modules require JDK 7. 
Further more, the SPDY protocol requires JDK 7.


## Hello App Walkthrough

The [Quick Start](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/quickstart) 
example app starts a web server on `localhost:8888` that returns _"Hello from Socko"_ in response to a HTTP GET request.

It illustrates the 3 steps that you have to follow to get Socko working for you.
 - [Step 1. Define Actors and Start Akka](#Step1)
 - [Step 2. Define Routes](#Step2)
 - [Step 3. Start/Stop Web Server](#Step3)

**HelloApp.scala**
{% highlight scala %}
  package org.mashupbots.socko.examples.quickstart

  import org.mashupbots.socko.routes._
  import org.mashupbots.socko.infrastructure.Logger
  import org.mashupbots.socko.webserver.WebServer
  import org.mashupbots.socko.webserver.WebServerConfig

  import akka.actor.ActorSystem
  import akka.actor.Props

  /**
   * This example shows how to setup a simple route and create a simple processor actor.
   *  - Run this class as a Scala Application
   *  - Open your browser and navigate to `http://localhost:8888/`
   *
   * Socko uses Netty to handle incoming requests and Akka to process them
   *  - Incoming requests are converted into Socko events using threads from the Netty thread pool
   *  - Your `routes` are then called to dispatch the event for processing
   *  - Inside our route definition, we instance a new `HelloHandler` actor and pass the event to it
   *  - The `HelloHandler` actor is executed in Akka default thread pool. This frees up the Netty thread pool to 
   *    undertake more networking activities.
   */
  object HelloApp extends Logger {
    //
    // STEP #1 - Define Actors and Start Akka
    // See `HelloHandler`
    //
    val actorSystem = ActorSystem("HelloExampleActorSystem")

    //
    // STEP #2 - Define Routes
    // Dispatch all HTTP GET events to a newly instanced `HelloHandler` actor for processing.
    // `HelloHandler` will `stop()` itself after processing each request.
    //
    val routes = Routes({
      case GET(request) => {
        actorSystem.actorOf(Props[HelloHandler]) ! request
      }
    })

    //
    // STEP #3 - Start and Stop Socko Web Server
    //
    def main(args: Array[String]) {
      val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
      webServer.start()

      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run { webServer.stop() }
      })

      System.out.println("Open your browser and navigate to http://localhost:8888")
    }
  }
{% endhighlight %}

**HelloHandler.scala**
{% highlight scala %}
  package org.mashupbots.socko.examples.quickstart

  import org.mashupbots.socko.events.HttpRequestEvent
  import akka.actor.Actor
  import java.util.Date

  /**
   * Hello processor writes a greeting and stops.
   */
  class HelloHandler extends Actor {
    def receive = {
      case event: HttpRequestEvent =>
        event.response.write("Hello from Socko (" + new Date().toString + ")")
        context.stop(self)
    }
  }
{% endhighlight %}


## Step 1. Define Actors and Start Akka <a class="blank" id="Step1"></a>

Socko assumes that you have your business rules implemented as Akka v2 Actors.

Incoming messages received by Socko will be wrapped within a {{ page.SockoEventClass }} and passed to your routes
for dispatching to your Akka actor handlers. Your actors use {{ page.SockoEventClass }} to read incoming data and 
write outgoing data.

In this exmaple, we have defined an actor called `HelloHandler` and started an Akka
system called `HelloExampleActorSystem`.  The `HttpRequestEvent` is used by the `HelloHandler`
to write a response to the client.
    

## Step 2. Define Routes <a class="blank" id="Step2"></a>

Routes allows you to control how Socko dispatches incoming events to your actors.

Routes are implemented in Socko using partial functions that take a {{ page.SockoEventClass }}
as input and returns `Unit` (or void).

Within your implementation of the partial function, your code will need to dispatch the 
{{ page.SockoEventClass }} to your intended actor for processing.

To assist with dispatching, we have included pattern matching extractors:

 - [Event](#SockoEventExtractors) such as {{ page.HttpRequestEventClass }} or {{ page.WebSocketFrameEventClass }}
 - [Host](#HostExtractors) such as `www.mydomain.com`
 - [Method](#MethodExtractors) such as `GET`
 - [Path](#PathExtractors) such as `/record/1`
 - [Query String](#QueryStringExtractors) such as `action=save`
 
[Concatenation](#ConcatenatingExtractors) of 2 or more extractors is also supported.
 
This example illustrates matching HTTP GET event and dispatching it to a `HelloHandler` actor.

For a more detailed example, see our [example route app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/routes).



## Step 3. Start/Stop Web Server <a class="blank" id="Step3"></a>

To start you web server, you only need to instance the {{ page.WebServerClass }} class and 
call `start()` passing in your configuration and routes.  When you wish to stop the web 
server, call `stop()`.

This example uses the default configuration which starts the web server at `localhost` bound on
port `8888`.  To customise, refer to [Configuration](configuration.html).




