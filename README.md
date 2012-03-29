# SOCKO

A lightweight web server that is written in [Scala](http://www.scala-lang.org/), 
runs on top of [Netty](http://netty.io/) and does its processing in [AKKA](http://akka.io/).

## Background
We designed MashupBots to be an HTML5 style app. 

Our fat html/css/javascript client is going to be served as static files from the web server. 
No server side HTML templating will be required because it will all happen in the browser using javascript.

Our fat client will communicate with the buisness logic/database via a HTTP REST base API served off 
the same web server.

We could not find a lightweight asynchronous web server that exactly meets our requirements so we decided 
to write one. (Besides, it is a great way to come up to speed with Scala and AKKA).

We hope you find it as useful as we do.

## What is Socko?

* Socko is written in Scala 

* Socko runs on top of the asynchronous event driven Netty framework.

* Socko expects all processing to be performed by AKKA actors.

* Socko has no external dependancies outside AKKA 2.0 (note that Netty is a dependancy of AKKA 2.0 Remote)

* Socko can be started as a standard Scala app.

* Out of the box, Socko supports
  * HTTP and WebSockets
  * TLS/SSL
  * Decoding HTTP POST and file uploads
  * HTTP compression
  * Routing like [Unfilted HTTP ToolKit](http://unfiltered.databinder.net/Unfiltered.html)
  * Serving of static files


## What Socko does not and will not support

* Socko does not run in a servlet
  
* Socko is not a web application or MVC framework like Lift or Play. It does not perform server based
  HTML templating. We belive that the view layer logic should be implemented in javascript and 
  executed in the browser.
    
* Socko does not store session data or caching of application data. HTML5 local storage provides for that.


## RoadMap

This project currently in development.



  
