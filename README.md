# SOCKO

## This repository is not maintained or supported by Asana, and is not accepting patches

Socko is an embedded [Scala](http://www.scala-lang.org/) web server powered by
[Netty](http://netty.io/) networking and [Akka](http://akka.io/) processing.

**Please see our [web site](http://sockoweb.org/) for documentaiton and more information**

## A quick example

```scala
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
```

## Editing the Source Code

* We are currently using [Eclipse Juno](http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/junosr1) 
  with [Scala IDE 3.0](http://scala-ide.org/download/current.html)

* We are currently using Scala 2.10.1 and JDK7. You will need JDK7 to build.
  The core and rest projects will be compiled for use with JDK6 but SPDY will 
  not be supported.

* Generate eclipse project files: `sbt eclispse`

* Start `Eclipse`

* From the top menu, select `File` | `Import`
  * Select `General` | `Existing Projects into Workspace`. 
  * Click `Next`.
  * Select the `socko` source code directory as the root
  * Should see `socko-examples` and `socko-webserver` under `Projects`
  * Click `Finish`

* To run the scalatest unit test cases, just right click on a test class file and select `Run As JUnit Test`.

## Getting Help

If you have a question or need help, please open a ticket in our [Issues Register] (https://github.com/mashupbots/socko/issues).

## Links

* [Web Site](http://sockoweb.org/)
* [Blog](http://sockoweb.org/blog)
* [Issues] (https://github.com/mashupbots/socko/issues/)
* [Road Map](https://github.com/mashupbots/socko/issues/milestones)
* [Examples](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples)

  
