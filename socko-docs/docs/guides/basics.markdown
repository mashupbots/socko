---
layout: docs
title: Socko User Guide - Basics

SockoEventClass: <code><a href="../api/#org.mashupbots.socko.events.SockoEvent">SockoEvent</a></code>
HttpRequestEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpRequestEvent">HttpRequestEvent</a></code>
HttpChunkEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpChunkEvent">HttpChunkEvent</a></code>
WebSocketFrameEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketFrameEvent">WebSocketFrameEvent</a></code>
WebSocketHandshakeEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketHandshakeEvent">WebSocketHandshakeEvent</a></code>
WebServerClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServer">WebServer</a></code>
---
# Socko User Guide - Basics

## Introduction

As illustrated in the [Quick Start](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/quickstart) 
example application, the 3 steps that you have to follow to get Socko working for you are:

 - [Step 1. Define Actors and Start Akka](#Step1)
   - [Handling Socko Events](#SockoEvents)
   - [Akka Dispatchers and Thread Pools](#AkkaDispatchers)
 - [Step 2. Define Routes](#Step2)
   - [Socko Event Extractors](#SockoEventExtractors)
   - [Host Extractors](#HostExtractors)
   - [Method Extractors](#MethodExtractors)
   - [Path Extractors](#PathExtractors)
   - [QueryString Extractors](#QueryStringExtractors)
   - [Concatenating Extractors](#ConcatenatingExtractors)
 - [Step 3. Start/Stop Web Server](#Step3)

These steps are specified in detailed below.


## Step 1. Define Actors and Start Akka <a class="blank" id="Step1">&nbsp;</a>

Socko assumes that you have your business rules implemented as Akka v2 Actors.

Incoming messages received by Socko will be wrapped within a {{ page.SockoEventClass }} and passed to your routes
for dispatching to your Akka actor handlers. Your actors use {{ page.SockoEventClass }} to read incoming data and 
write outgoing data.

In the following `HelloApp` example, we have defined an actor called `HelloHandler` and started an Akka
system called `HelloExampleActorSystem`.  The `HttpRequestEvent` is used by the `HelloHandler`
to write a response to the client.

{% highlight scala %}
    object HelloApp extends Logger {
      //
      // STEP #1 - Define Actors and Start Akka
      // See `HelloHandler`
      //
      val actorSystem = ActorSystem("HelloExampleActorSystem")
    }
    
    /**
     * Hello processor writes a greeting and stops.
     */
    class HelloHandler extends Actor {
      def receive = {
        case request: HttpRequestEvent =>
          request.writeResponse("Hello from Socko (" + new Date().toString + ")")
          context.stop(self)
      }
    }
{% endhighlight %}
    
For maximum scalability and performance, you will need to carefully choose your Akka dispatchers.
The default dispatcher is optimized for non blocking code. If your code blocks though reading from and writing to 
database and/or file system, then it is advisable to configure Akka to use dispatchers based on thread pools.

### Handling Socko Events <a class="blank" id="SockoEvents">&nbsp;</a>

A {{ page.SockoEventClass }} is used to read incoming and write outgoing data.

Your Actor handler must be able to handle {{ page.SockoEventClass }}s.

Two ways to achieve this are:

1. You can change your actors to be Socko {{ page.SockoEventClass }} aware by adding a {{ page.SockoEventClass }} 
   property to messages that it receives.

2. You can write a facade actor to specifically handle {{ page.SockoEventClass }}. Your facade can
   read the request from the {{ page.SockoEventClass }} in order to create messages to pass to your actors
   for processing. Your facade actor could store the {{ page.SockoEventClass }} to use it to write responses.

There are 4 types of {{ page.SockoEventClass }}:

1. **{{ page.HttpRequestEventClass }}**

   This event is fired when a HTTP Request is received.
   
   To read the request, use `request.content.toString()` or `request.content.toBytes()`. Refer to the [file upload example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/fileupload)
   for the decoding of HTTP post data.
   
   To write a response, use `response.write()`. If you wish to stream your response, you will need to use 
   `response.writeFirstChunk()`, `response.writeChunk()` and `response.writeLastChunk()` instead. 
   Refer to the [streaming example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/streaming)
   for usage.

2. **{{ page.HttpChunkEventClass }}**

   This event is fired when a HTTP Chunk is received and is only applicable if you turn off 
   [chunk aggregation](#Configuration).
   
   Reading requests and writing responses is as per {{ page.HttpRequestEventClass }}.

3. **{{ page.WebSocketFrameEventClass }}**

   This event is fired when a Web Socket Frame is received.
   
   To read a frame, first check if it `isText` or `isBinary`.  If text, use `readText()`. If binary, use 
   `readBinary()`.
   
   To write a frame, use `writeText()` or `writeBinary()`.

4. **{{ page.WebSocketHandshakeEventClass }}**

   This event is fired for Web Socket handshaking within your [Route](#Step2). 

   It should **not** be sent to your actor.


All {{ page.SockoEventClass }} must be used by **local actors** only.


### Akka Dispatchers and Thread Pools <a class="blank" id="AkkaDispatchers">&nbsp;</a>

Akka [dispatchers](http://doc.akka.io/docs/akka/2.0.1/scala/dispatchers.html) controls how your Akka 
actors process messages.

Akka's default dispatcher is optimized for non blocking code.

However, if your actors have blocking operations like database read/write or file system read/write, 
we recommend that you run these actors with a different dispatcher.  In this way, while these actors block a thread,
other actors can continue processing on other threads.


The following code is taken from our [file upload example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/fileupload).
Because `StaticContentHandler` and `FileUploadHandler` actors read and write lots of files, we have set them up 
to use a `PinnedDispatcher`. Note that we have only allocated 5 threads to each processor. To scale, you may wish 
to allocate more threads.

{% highlight scala %}
    val actorConfig = """
      my-pinned-dispatcher {
        type=PinnedDispatcher
        executor=thread-pool-executor
      }
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel=DEBUG
        actor {
          deployment {
            /static-file-router {
              router = round-robin
              nr-of-instances = 5
            }
            /file-upload-router {
              router = round-robin
              nr-of-instances = 5
            }
          }
        }
      }"""

    val actorSystem = ActorSystem("FileUploadExampleActorSystem", ConfigFactory.parseString(actorConfig))

    val staticContentHandlerRouter = actorSystem.actorOf(Props[StaticContentHandler]
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")
    
    val fileUploadHandlerRouter = actorSystem.actorOf(Props[FileUploadHandler]
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "file-upload-router")
{% endhighlight %}




## Step 2. Define Routes <a class="blank" id="Step2">&nbsp;</a>

Routes allows you to control how Socko dispatches incoming events to your actors.

Routes are implemented in Socko using partial functions that take a {{ page.SockoEventClass }}
as input and returns `Unit` (or void).

Within your implementation of the partial function, your code will need to dispatch the 
{{ page.SockoEventClass }} to your intended actor for processing.

To assist with dispatching, we have included pattern matching extractors:

 - [Event](#SockoEventExtractors)
 - [Host](#HostExtractors) such as `www.mydomain.com`
 - [Method](#MethodExtractors) such as `GET`
 - [Path](#PathExtractors) such as `/record/1`
 - [Query String](#QueryStringExtractors) such as `action=save`
 
[Concatenation](#ConcatenatingExtractors) of 2 or more extractors is also supported.
 
The following example illustrates matching HTTP GET event and dispatching it to a `HelloHandler` actor:

{% highlight scala %}
    val routes = Routes({
      case GET(request) => {
        actorSystem.actorOf(Props[HelloHandler]) ! request
      }
    })
{% endhighlight %}

For a more detailed example, see our [example route app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/routes).


### Event Extractors <a class="blank" id="SockoEventExtractors">&nbsp;</a>

These extractors allows you to match different types of {{ page.SockoEventClass }}.

 - [`HttpRequest`](../api/#org.mashupbots.socko.routes.HttpRequest$) matches {{ page.HttpRequestEventClass }}
 - [`HttpChunk`](../api/#org.mashupbots.socko.routes.HttpChunk$) matches {{ page.HttpChunkEventClass }}
 - [`WebSocketFrame`](../api/#org.mashupbots.socko.routes.WebSocketFrame$) matches {{ page.WebSocketFrameEventClass }}
 - [`WebSocketHandshake`](../api/#org.mashupbots.socko.routes.WebSocketHandshake$) matches {{ page.WebSocketHandshakeEventClass }}

The following code taken from our [web socket example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/websocket) 
illustrates usage:

{% highlight scala %}
    val routes = Routes({
    
      case HttpRequest(httpRequest) => httpRequest match {
        case GET(Path("/html")) => {
          // Return HTML page to establish web socket
          actorSystem.actorOf(Props[WebSocketHandler]) ! httpRequest
        }
        case Path("/favicon.ico") => {
          // If favicon.ico, just return a 404 because we don't have that file
          httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
        }
      }
      
      case WebSocketHandshake(wsHandshake) => wsHandshake match {
        case Path("/websocket/") => {
          // To start Web Socket processing, we first have to authorize the handshake.
          // This is a security measure to make sure that web sockets can only be established at your specified end points.
          wsHandshake.authorize()
        }
      }
    
      case WebSocketFrame(wsFrame) => {
        // Once handshaking has taken place, we can now process frames sent from the client
        actorSystem.actorOf(Props[WebSocketHandler]) ! wsFrame
      }
    
    })
{% endhighlight %}


### Host Extractors <a class="blank" id="HostExtractors">&nbsp;</a>

Host extractors matches the host name received in the HTTP request that triggered the {{ page.SockoEventClass }}.

For {{ page.HttpRequestEventClass }}, the host is the value specified in the `HOST` header variable. 
For {{ page.HttpChunkEventClass }}, {{ page.WebSocketFrameEventClass }} and 
{{ page.WebSocketHandshakeEventClass }}, the host is that of the associated initial 
{{ page.HttpRequestEventClass }}.

For example, the following HTTP request has a host value of `www.sockoweb.org`:

    GET /index.html HTTP/1.1
    Host: www.sockoweb.org


**[`Host`](../api/#org.mashupbots.socko.routes.Host$)**

Performs an exact match on the specified host.

The following example will match `www.sockoweb.org` but not: `www1.sockoweb.org`, `sockoweb.com` or `sockoweb.org`.

{% highlight scala %}
    val r = Routes({
      case Host("www.sockoweb.org") => {
        ...
      }
    })
{% endhighlight %}


**[`HostSegments`](../api/#org.mashupbots.socko.routes.HostSegments$)**

Performs matching and variable binding on segments of a host. Each segment is assumed to be delimited
by a period.

For example:

{% highlight scala %}
    val r = Routes({
      // Matches server1.sockoweb.org
      case HostSegments(server :: "sockoweb" :: "org" :: Nil) => {
        ...
      }
    })
{% endhighlight %}

This will match any hosts that have 3 segments and the last 2 segments being `sockoweb.org`. 
The first segment will be bound to a variable called `server.` 

This will match `www.sockoweb.org` and the `server` variable have a value of `www`.

This will NOT match `www.sockoweb.com` because it ends in `.com`; or `sockweb.org` because there 
are only 2 segments.

 
**[`HostRegex`](../api/#org.mashupbots.socko.routes.HostRegex)**

Matches the host based on a regular expression pattern.

For example, to match `www.anydomainname.com`, first define your regular expression as an object and then use it
in your route.

{% highlight scala %}
    object MyHostRegex extends HostRegex("""www\.([a-z]+)\.com""".r)
    
    val r = Routes({
      // Matches www.anydomainname.com
      case MyHostRegex(m) => {
        assert(m.group(1) == "anydomainname")
        ...
      }
    })
{% endhighlight %}


### Method Extractors <a class="blank" id="MethodExtractors">&nbsp;</a>

Method extractors matches the method received in the HTTP request that triggered the {{ page.SockoEventClass }}.

For {{ page.HttpRequestEventClass }}, the method is the extracted from the 1st line. 
For {{ page.HttpChunkEventClass }}, {{ page.WebSocketFrameEventClass }} and 
{{ page.WebSocketHandshakeEventClass }}, the method is that of the associated initial 
{{ page.HttpRequestEventClass }}.

For example, the following HTTP request has a method `GET`:

    GET /index.html HTTP/1.1
    Host: www.sockoweb.org

Socko supports the following methods:

 - [`GET`](../api/#org.mashupbots.socko.routes.GET$)
 - [`POST`](../api/#org.mashupbots.socko.routes.POST$)
 - [`PUT`](../api/#org.mashupbots.socko.routes.PUT$)
 - [`DELETE`](../api/#org.mashupbots.socko.routes.DELETE$)
 - [`CONNECT`](../api/#org.mashupbots.socko.routes.CONNECT$)
 - [`HEAD`](../api/#org.mashupbots.socko.routes.HEAD$)
 - [`TRACE`](../api/#org.mashupbots.socko.routes.TRACE$)

For example, to match every HTTP GET:

{% highlight scala %}
    val r = Routes({
      case GET(_) => {
        ...
      }
    })
{% endhighlight %}
  
Method extractors return the {{ page.SockoEventClass }} so it can be used with other extractors
in the same case statement. 

For example, to match HTTP GET with a path of "/clients"

{% highlight scala %}
    val r = Routes({
      case GET(Path("/clients")) => {
        ...
      }
    })
{% endhighlight %}



### Path Extractors <a class="blank" id="PathExtractors">&nbsp;</a>

Path extractors matches the path received in the HTTP request that triggered the {{ page.SockoEventClass }}.

For {{ page.HttpRequestEventClass }}, the path is the extracted from the 1st line without any query string. 
For {{ page.HttpChunkEventClass }}, {{ page.WebSocketFrameEventClass }} and 
{{ page.WebSocketHandshakeEventClass }}, the path is that of the associated initial 
{{ page.HttpRequestEventClass }}.

For example, the following HTTP requests have a path value of `/index.html`:

    GET /index.html HTTP/1.1
    Host: www.sockoweb.org
    
    GET /index.html?name=value HTTP/1.1
    Host: www.sockoweb.org


**[`Path`](../api/#org.mashupbots.socko.routes.Path$)**

Performs an exact match on the specified path.

The following example will match `folderX` but not: `/folderx`, `/folderX/` or `/TheFolderX`.

{% highlight scala %}
    val r = Routes({
      case Path("/folderX") => {
        ...
      }
    })
{% endhighlight %}


**[`PathSegments`](../api/#org.mashupbots.socko.routes.PathSegments$)**

Performs matching and variable binding on segments of a path. Each segment is assumed to be delimited
by a slash.

For example:

{% highlight scala %}
    val r = Routes({
      // Matches /record/1
      case PathSegments("record" :: id :: Nil) => {
        ...
      }
    })
{% endhighlight %}

This will match any paths that have 2 segments and the first segment being `record`. The second 
segment will be bound to a variable called `id.` 

This will match `/record/1` and `id` will be set to `1`.

This will NOT match `/record` because there is only 1 segment; or `/folder/1` before the first segment
is not `record`.

Another example:

{% highlight scala %}
    val r = Routes({
      // Matches /api/abc and /api/xyz
      case PathSegments("abc" :: relativePath) => {
        ...
      }
    })
{% endhighlight %}

This will match any paths that has the first segment set as `api`. The remainder of the path segments
will be boudn to the variable `relativePath`.

This will match `/api/abc` and `relativePath` will be set to `List(abc)`.

This will NOT match `/aaa/abc` because the first segment is not `api`.

 
**[`PathRegex`](../api/#org.mashupbots.socko.routes.PathRegex)**

Matches the path based on a regular expression pattern.

For example, to match `/path/to/file`, first define your regular expression as an object and then use it
in your route.

{% highlight scala %}
    object MyPathRegex extends PathRegex("""/path/([a-z0-9]+)/([a-z0-9]+)""".r)
    
    val r = Routes({
      // Matches /path/to/file
      case MyPathRegex(m) => {
        assert(m.group(1) == "to")
        assert(m.group(2) == "file")
        ...
      }
    })
{% endhighlight %}


### Query String Extractors <a class="blank" id="QueryStringExtractors">&nbsp;</a>

Query string extractors matches the query string received in the HTTP request that triggered the {{ page.SockoEventClass }}.

For {{ page.HttpRequestEventClass }}, the query string is the extracted from the 1st line. 
For {{ page.HttpChunkEventClass }}, {{ page.WebSocketFrameEventClass }} and 
{{ page.WebSocketHandshakeEventClass }}, the query string is that of the associated initial HTTP Request.

For example, the following HTTP request has a query string value of `name=value`:

    GET /index.html?name=value HTTP/1.1
    Host: www.sockoweb.org


**[`QueryString`](../api/#org.mashupbots.socko.routes.QueryString$)**

Performs an exact match on the query string.

The following example will match `action=save` but not: `action=view` or `action=save&id=1`.

{% highlight scala %}
    val r = Routes({
      case QueryString("action=save") => {
        ...
      }
    })
{% endhighlight %}


**[`QueryStringField`](../api/#org.mashupbots.socko.routes.QueryStringField)**

Performs matching and variable binding a query string value for a specified query string field.

For example, to match whenever the `action` field is present and bind its value to a variable called
`actionValue`:

{% highlight scala %}
    object ActionQueryStringField extends QueryStringName("action")
    
    val r = Routes({
      // Matches '?action=save' and actionValue will be set to 'save'
      case ActionQueryStringField(actionValue) => {
        ...
      }
    })
{% endhighlight %}

**[`QueryStringRegex`](../api/#org.mashupbots.socko.routes.QueryStringRegex)**

Matches the query string based on a regular expression pattern.

For example, to match `?name1=value1`:

{% highlight scala %}
    object MyQueryStringRegex extends QueryStringRegex("""name1=([a-z0-9]+)""".r)
    
    val r = Routes({
      // Matches /path/to/file
      case MyQueryStringRegex(m) => {
        assert(m.group(1) == "value1")
        ...
      }
    })
{% endhighlight %}


### Concatenation Extractors <a class="blank" id="ConcatenatingExtractors">&nbsp;</a>

At times, it is useful to combine 2 or more extractors in a single case statement. For this, you can
use an ampersand ([`&`](../api/#org.mashupbots.socko.routes.$amp$)). 

For example, if you wish to match a path of `/record/1` and a query string of `action=save`,
you can use 

{% highlight scala %}
    object ActionQueryStringField extends QueryStringName("action")
    
    val r = Routes({
      case PathSegments("record" :: id :: Nil) & ActionQueryStringField(actionValue) => {
        ...
      }
    })
{% endhighlight %}




## Step 3. Start/Stop Web Server <a class="blank" id="Step3">&nbsp;</a>

To start you web server, you only need to instance the {{ page.WebServerClass }} class and 
call `start()` passing in your configuration and routes.  When you wish to stop the web 
server, call `stop()`.

{% highlight scala %}
    def main(args: Array[String]) {
      val webServer = new WebServer(WebServerConfig(), routes)
      webServer.start()
  
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run { webServer.stop() }
      })
    
      System.out.println("Open your browser and navigate to http://localhost:8888")
    }
{% endhighlight %}

This example uses the default configuration which starts the web server at `localhost` bound on
port `8888`.  To customise, refer to [Configuration](configuration.html).


