# SOCKO

A lightweight [Scala](http://www.scala-lang.org/) web server powered by
[Netty](http://netty.io/) networking and [AKKA](http://akka.io/) processing.

## Background
We designed MashupBots to be an HTML5 style app. 

Our fat html/css/javascript client is going to be served as static files from the web server. 
Server side HTML templating will be not required because it will all happen in the browser using javascript.

Our fat client will communicate with the business logic/database via a HTTP REST based API served off 
the same web server.

We could not find a lightweight asynchronous web server that exactly meets our requirements so we decided 
to write one. (Besides, it is a great way to come up to speed with Scala and AKKA).

We hope you find it as useful as we do.

## Features

* Socko is written in Scala 

* Socko runs on top of the asynchronous event driven Netty framework.

* Socko processes requests using AKKA actors.

* Socko has no external dependencies outside AKKA 2.0 (note that Netty is a dependency of AKKA 2.0 Remote)

* Socko can be started as a standard Scala application.

* Out of the box, Socko supports
  * HTTP and WebSockets
  * TLS/SSL
  * Decoding HTTP POST and file uploads
  * HTTP compression
  * Routing like [Unfilted HTTP ToolKit](http://unfiltered.databinder.net/Unfiltered.html) and 
    [Play Mini](https://github.com/typesafehub/play2-mini)
  * Serving of static files


## Anti-Features

* Socko is not a servlet. We use Netty instead.
  
* Socko is not a web application or MVC framework like Lift or Play. It does not perform server side
  HTML templating. We use client side javascript libraries like EmberJS and BackboneJS instead.
    
* Socko does not store session data or cache application data. We use HTML5 local storage instead.


## Quick Start

Coming soon


## RoadMap

This project currently in development.



  
