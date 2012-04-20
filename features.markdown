---
layout: article
---
#Socko Features

## Intended for Scala and Akka developers
Socko enables you to expose your Akka actors as HTTP or WebSocket endpoints.

It uses a routing DSL like [Unfilted](http://unfiltered.databinder.net/Unfiltered.html) and 
[Play2 Mini](https://github.com/typesafehub/play2-mini). Route by HTTP method, host, path and querystring.
Here's an [example](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/routes/RouteApp.scala).

Socko includes a StaticFileProcessor Akka actor that serves static files.  We use this to serve
our HTML5 application files.

We did consider using an existing Scala web servers. Here's why they did not suit are needs:
 * [Play2](http://www.playframework.org/), [Lift](http://liftweb.net/), [Xitrum](https://github.com/ngocdaothanh/xitrum), [Sweet](http://code.google.com/p/sweetscala/) - we do not need server based web application framework
 * [Play2-mini](https://github.com/typesafehub/play2-mini) - a bit too heavyweigth as of version 1 because it pulls in all of Play2.
 * [Spray](https://github.com/spray/spray/wiki), [Unfiltered](http://unfiltered.databinder.net/Unfiltered.html) - we needed WebSocket support
 * [Scalatra](http://www.scalatra.org/) - we did not want to use a servlet container
 
**NOTE**

We are not saying that any of the above great frameworks are in anyway bad.

What we are saying is they did not exactly fit the requirements of our mashupbots project.  With our existing 
knowledge of [Netty](http://netty.io), we thought it was quicker for us to build Socko than to modify/enhance 
an existing project to suite our needs.


## Embedded
Socko runs within your Scala application. It is not a standalone web server.

You can configure Socko from inside your code and/or via settings in Akka's configuration file.

You can also start more than web server instance in your application; bound to different ports. This is useful
if you have APIs that you wish to publish on different network interfaces; for exmaple a public API and a 
private administraion/monitoring API.


## Lightweight (assuming you are already using Akka)
V0.1 has approximately 1,300 lines of Scala code (and ~2,600 lines of Java code which will be removed once Akka 
moves to the upcoming Netty 4.0).
    
Socko has no external dependencies outside Akka 2.0 Remote (which includes Netty).


## Supportive of HTTP and HTML5 Standards
 * HTTP/S and WebSockets
  
 * HTTP compression
 
 * HTTP streaming (i.e. "chunked" transfer encoding)

 * HTTP/1.1 persistant connections (keep-alive)

 * HTTP headers, including browser static file cache headers
 
 * Decoding HTTP POST request body, file uploads and query strings


## Fast-ish
Socko handles and processes incoming HTTP requests in an asynchronous and event driven manner thanks to
Netty and Akka.

We are aiming to be at least on-par with Apache Tomcat 7.

We still have some more benchmarking to do, but early testing indicates that we are going to meet this objective.


## Open Source
Socko is published under the [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0).

The source code is hosted at [github](https://github.com/mashupbots/socko).

If you have a bug fix or enhancements, please send us a github pull requst.  Your contribution is welcome and
appreciated.



