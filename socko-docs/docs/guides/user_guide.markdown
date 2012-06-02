---
layout: docs
title: Socko User Guide

ProcessingContextClass: <code><a href="../api/#org.mashupbots.socko.context.ProcessingContext">ProcessingContext</a></code>
HttpRequestContextClass: <code><a href="../api/#org.mashupbots.socko.context.HttpRequestContext">HttpRequestContext</a></code>
HttpChunkContextClass: <code><a href="../api/#org.mashupbots.socko.context.HttpChunkContext">HttpChunkContext</a></code>
WebSocketFrameContextClass: <code><a href="../api/#org.mashupbots.socko.context.WebSocketFrameContext">WebSocketFrameContext</a></code>
WebSocketHandshakeContextClass: <code><a href="../api/#org.mashupbots.socko.context.WebSocketHandshakeContext">WebSocketHandshakeContext</a></code>
WebServerClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServer">WebServer</a></code>
WebServerConfigClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServerConfig">WebServerConfig</a></code>
WebLogEventClass: <code><a href="../api/#org.mashupbots.socko.utils.WebLogEvent">WebLogEvent</a></code>
WebLogQueueClass: <code><a href="../api/#org.mashupbots.socko.utils.WebLogQueue">WebLogQueue</a></code>
DefaultWebLogQueueClass: <code><a href="../api/#org.mashupbots.socko.utils.DefaultWebLogQueue">DefaultWebLogQueue</a></code>
DefaultWebLogWriterClass: <code><a href="../api/#org.mashupbots.socko.utils.DefaultWebLogWriter">DefaultWebLogWriter</a></code>
---
# Socko User Guide

## Table of Contents

 - [Step 1. Define Actors and Start Akka.](#Step1)
 - [Step 2. Define Routes.](#Step2)
 - [Step 3. Start/Stop Web Server.](#Step3)
 - [Configuration](#Configuration)
 - [Web Logs](#WebLogs)
 - [Code Examples](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples)



## Step 1. Define Actors and Start Akka <a class="blank" id="Step1">&nbsp;</a>

Socko assumes that you have your business rules implemented as Akka v2 Actors.

Requests received by Socko will be passed to your actors within a Socko {{ page.ProcessingContextClass }}.
Your actors use {{ page.ProcessingContextClass }} to read the request and write a response.

In the following `HelloApp` example, we have defined an actor called `HelloProcessor` and started an Akka
system called `HelloExampleActorSystem`.  The `HttpRequestContext` is used by `HelloProcessor`
to write a response to the client.

    object HelloApp extends Logger {
      //
      // STEP #1 - Define Actors and Start Akka
      // See `HelloProcessor`
      //
      val actorSystem = ActorSystem("HelloExampleActorSystem")
    }
    
    /**
     * Hello processor writes a greeting and stops.
     */
    class HelloProcessor extends Actor {
      def receive = {
        case request: HttpRequestContext =>
          request.writeResponse("Hello from Socko (" + new Date().toString + ")")
          context.stop(self)
      }
    }
    
For maximum scalability and performance, you will need to carefully choose your Akka dispatchers.
The default dispatcher is optimized for non blocking code. If your code blocks though execution of 
database, file system and/or network IO, then it is advisable to configure Akka to use dispatchers based
on thread pools.

### Processing Context
In order to read requests and writes responses, your actors must use Socko's {{ page.ProcessingContextClass }}.

Two ways to achieve this are:

1. You can change your actors to be Socko {{ page.ProcessingContextClass }} aware by adding a {{ page.ProcessingContextClass }} 
   property to messages that it receives - as per above example.

2. You can write a facade actor to specifically handle Socko {{ page.ProcessingContextClass }}. Your facade can
   read the request from the {{ page.ProcessingContextClass }} in order to create messages to pass to your actors
   for processing. Your facade should also setup Akka [futures](http://doc.akka.io/docs/akka/2.0.1/scala/futures.html)
   so that when processing is completed, the {{ page.ProcessingContextClass }} can be used to write your response.

There are 4 types of {{ page.ProcessingContextClass }}:

1. **{{ page.HttpRequestContextClass }}**

   This context will be sent to your actor when a HTTP Request is received.
   
   To read the request, use `request.content.toString()` or `request.content.toBytes()`. Refer to the [file upload example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/fileupload)
   for the decoding of HTTP post data.
   
   To write a response, use `response.write()`. If you wish to stream your response, you will need to use 
   `response.writeFirstChunk()`, `writeChunk()` and `writeLastChunk()` instead. Refer to the [streaming example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/streaming)
   for usage.

2. **{{ page.HttpChunkContextClass }}**

   This context will be sent to your actor when a HTTP Chunk is received and is only applicable if you turn off 
   [chunk aggregation](#Configuration).
   
   Reading requests and writing responses is as per {{ page.HttpRequestContextClass }}.

3. **{{ page.WebSocketFrameContextClass }}**

   This context will be sent to your actor when a Web Socket Frame is received.
   
   To read a frame, first check if it `isText` or `isBinary`.  If text, use `readText()`. If binary, use 
   `readBinary()`.
   
   To write a frame, use `writeText()` or `writeBinary()`.

4. **{{ page.WebSocketHandshakeContextClass }}**

   This context is only used for Web Socket handshaking within your [Route](#Step2). 

   It should **not** be sent to your actor.


All {{ page.ProcessingContextClass }} must be used by **local actors** only.

### Akka Dispatchers and Thread Pools

Akka [dispatchers](http://doc.akka.io/docs/akka/2.0.1/scala/dispatchers.html) controls how your Akka 
actors processes messages.

The default dispatcher is optimized for non blocking code.

However, if your actors have blocking operations like database read/write or file system read/write, 
we recommend that you use a dispatcher driven by the `thread-pool-executor`.  For example, the 
`PinnedDispatcher` is used in Socko's file upload [example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/fileupload).

Isolating blocking operations to a thread pool means that other actors can continue processing without
waiting for your actor's blocking operation to finish.

The following code is taken from our file upload example application. Because `StaticFileProcessor`
and `FileUploadProcessor` reads and writes lots of files, we have set them up to use a `PinnedDispatcher`.
Note that we have only allocated 5 threads to each processor. To scale, you will need to allocate
more threads.

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

    val staticFileProcessorRouter = actorSystem.actorOf(Props[StaticFileProcessor]
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")
    
    val fileUploadProcessorRouter = actorSystem.actorOf(Props[FileUploadProcessor]
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "file-upload-router")





## Step 2. Define Routes <a class="blank" id="Step2">&nbsp;</a>

Routes allows you to control how Socko dispatches incoming requests to your actors.

Routes are implemented in Socko using partial functions that take a {{ page.ProcessingContextClass }}
as input and returns `Unit` (or void).

Within your implementation of the partial function, your code will need to dispatch the 
{{ page.ProcessingContextClass }} to your intended actor for processing.

To assist with dispatching, we have included extractors that can match the type of {{ page.ProcessingContextClass }}

 - [Processing Context](#ProcessingContextExtractors)
 - [Host](#HostExtractors) such as `www.mydomain.com`
 - [Method](#MethodExtractors) such as `GET`
 - [Path](#PathExtractors) such as `/record/1`
 - [Query String](#QueryStringExtractors) such as `action=save`
 
[Concatenation](#ConcatenatingExtractors) of 2 or more extractors is also supported.
 
The following example illustrates matching HTTP GET requests and dispatching the request to a 
`HelloProcessor` actor:

    val routes = Routes({
      case GET(request) => {
        actorSystem.actorOf(Props[HelloProcessor]) ! request
      }
    })

For a more detailed example, see our [example route app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/routes).


### Processing Context Extractors <a class="blank" id="ProcessingContextExtractors">&nbsp;</a>

These extractors allows you to match different types of {{ page.ProcessingContextClass }}.

 - [`HttpRequest`](../api/#org.mashupbots.socko.routes.HttpRequest$) matches {{ page.HttpRequestContextClass }}
 - [`HttpChunk`](../api/#org.mashupbots.socko.routes.HttpChunk$) matches {{ page.HttpChunkContextClass }}
 - [`WebSocketFrame`](../api/#org.mashupbots.socko.routes.WebSocketFrame$) matches {{ page.WebSocketFrameContextClass }}
 - [`WebSocketHandshake`](../api/#org.mashupbots.socko.routes.WebSocketHandshake$) matches {{ page.WebSocketHandshakeContextClass }}

The following code taken from our [web socket example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/websocket) 
illustrates usage:

    val routes = Routes({
    
      case HttpRequest(httpRequest) => httpRequest match {
        case GET(Path("/html")) => {
          // Return HTML page to establish web socket
          actorSystem.actorOf(Props[WebSocketProcessor]) ! httpRequest
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
        actorSystem.actorOf(Props[WebSocketProcessor]) ! wsFrame
      }
    
    })

Note that for a web socket handshake, you only need to set the context's `isAllowed` property to true.
Dispatching to an actor is not required and not recommended.

### Host Extractors <a class="blank" id="HostExtractors">&nbsp;</a>

Host extractors matches the host name received in the request.

For {{ page.HttpRequestContextClass }}, the host is the value specified in the `HOST` header variable. 
For {{ page.HttpChunkContextClass }}, {{ page.WebSocketFrameContextClass }} and 
{{ page.WebSocketHandshakeContextClass }}, the host is that of the associated initial 
{{ page.HttpRequestContextClass }}.

For example, the following HTTP request has a host value of `www.sockoweb.org`:

    GET /index.html HTTP/1.1
    Host: www.sockoweb.org


**[`Host`](../api/#org.mashupbots.socko.routes.Host$)**

Performs an exact match on the specified host.

The following example will match `www.sockoweb.org` but not: `www1.sockoweb.org`, `sockoweb.com` or `sockoweb.org`.

    val r = Routes({
      case Host("www.sockoweb.org") => {
        ...
      }
    })


**[`HostSegments`](../api/#org.mashupbots.socko.routes.HostSegments$)**

Performs matching and variable binding on segments of a host. Each segment is assumed to be delimited
by a period.

For example:

    val r = Routes({
      // Matches server1.sockoweb.org
      case HostSegments(server :: "sockoweb" :: "org" :: Nil) => {
        ...
      }
    })

This will match any hosts that have 3 segments and the last 2 segments being `sockoweb.org`. 
The first segment will be bound to a variable called `server.` 

This will match `www.sockoweb.org` and the `server` variable have a value of `www`.

This will NOT match `www.sockoweb.com` because it ends in `.com`; or `sockweb.org` because there 
are only 2 segments.

 
**[`HostRegex`](../api/#org.mashupbots.socko.routes.HostRegex)**

Matches the host based on a regular expression pattern.

For example, to match `www.anydomainname.com`, first define your regular expression as an object and then use it
in your route.

    object MyHostRegex extends HostRegex("""www\.([a-z]+)\.com""".r)
    
    val r = Routes({
      // Matches www.anydomainname.com
      case MyHostRegex(m) => {
        assert(m.group(1) == "anydomainname")
        ...
      }
    })


### Method Extractors <a class="blank" id="MethodExtractors">&nbsp;</a>

Method extractors matches the method received in the request.

For {{ page.HttpRequestContextClass }}, the method is the extracted from the 1st line. 
For {{ page.HttpChunkContextClass }}, {{ page.WebSocketFrameContextClass }} and 
{{ page.WebSocketHandshakeContextClass }}, the method is that of the associated initial 
{{ page.HttpRequestContextClass }}.

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

    val r = Routes({
      case GET(_) => {
        ...
      }
    })
  
Method extractors return the {{ page.ProcessingContextClass }} so it can be used with other extractors
in the same case statement. 

For example, to match HTTP GET with a path of "/clients"

    val r = Routes({
      case GET(Path("/clients")) => {
        ...
      }
    })



### Path Extractors <a class="blank" id="PathExtractors">&nbsp;</a>

Path extractors matches the path received in the request.

For {{ page.HttpRequestContextClass }}, the path is the extracted from the 1st line without any query string. 
For {{ page.HttpChunkContextClass }}, {{ page.WebSocketFrameContextClass }} and 
{{ page.WebSocketHandshakeContextClass }}, the path is that of the associated initial 
{{ page.HttpRequestContextClass }}.

For example, the following HTTP requests have a path value of `/index.html`:

    GET /index.html HTTP/1.1
    Host: www.sockoweb.org
    
    GET /index.html?name=value HTTP/1.1
    Host: www.sockoweb.org


**[`Path`](../api/#org.mashupbots.socko.routes.Path$)**

Performs an exact match on the specified path.

The following example will match `folderX` but not: `/folderx`, `/folderX/` or `/TheFolderX`.

    val r = Routes({
      case Path("/folderX") => {
        ...
      }
    })


**[`PathSegments`](../api/#org.mashupbots.socko.routes.PathSegments$)**

Performs matching and variable binding on segments of a path. Each segment is assumed to be delimited
by a slash.

For example:

    val r = Routes({
      // Matches /record/1
      case PathSegments("record" :: id :: Nil) => {
        ...
      }
    })

This will match any paths that have 2 segments and the first segment being `record`. The second 
segment will be bound to a variable called `id.` 

This will match `/record/1` and `id` will be set to `1`.

This will NOT match `/record` because there is only 1 segment; or `/folder/1` before the first segment
is not `record`.

 
**[`PathRegex`](../api/#org.mashupbots.socko.routes.PathRegex)**

Matches the path based on a regular expression pattern.

For example, to match `/path/to/file`, first define your regular expression as an object and then use it
in your route.

    object MyPathRegex extends PathRegex("""/path/([a-z0-9]+)/([a-z0-9]+)""".r)
    
    val r = Routes({
      // Matches /path/to/file
      case MyPathRegex(m) => {
        assert(m.group(1) == "to")
        assert(m.group(2) == "file")
        ...
      }
    })


### Query String Extractors <a class="blank" id="QueryStringExtractors">&nbsp;</a>

Query string extractors matches the query string received in the request.

For {{ page.HttpRequestContextClass }}, the query string is the extracted from the 1st line. 
For {{ page.HttpChunkContextClass }}, {{ page.WebSocketFrameContextClass }} and 
{{ page.WebSocketHandshakeContextClass }}, the query string is that of the associated initial HTTP Request.

For example, the following HTTP request has a query string value of `name=value`:

    GET /index.html?name=value HTTP/1.1
    Host: www.sockoweb.org


**[`QueryString`](../api/#org.mashupbots.socko.routes.QueryString$)**

Performs an exact match on the query string.

The following example will match `action=save` but not: `action=view` or `action=save&id=1`.

    val r = Routes({
      case QueryString("action=save") => {
        ...
      }
    })


**[`QueryStringField`](../api/#org.mashupbots.socko.routes.QueryStringField)**

Performs matching and variable binding a query string value for a specified query string field.

For example, to match whenever the `action` field is present and bind its value to a variable called
`actionValue`:

    object ActionQueryStringField extends QueryStringName("action")
    
    val r = Routes({
      // Matches '?action=save' and actionValue will be set to 'save'
      case ActionQueryStringField(actionValue) => {
        ...
      }
    })
 
**[`QueryStringRegex`](../api/#org.mashupbots.socko.routes.QueryStringRegex)**

Matches the query string based on a regular expression pattern.

For example, to match `?name1=value1`:

    object MyQueryStringRegex extends QueryStringRegex("""name1=([a-z0-9]+)""".r)
    
    val r = Routes({
      // Matches /path/to/file
      case MyQueryStringRegex(m) => {
        assert(m.group(1) == "value1")
        ...
      }
    })


### Concatenation Extractors <a class="blank" id="ConcatenatingExtractors">&nbsp;</a>

At times, it is useful to combine 2 or more extractors in a single case statement. For this, you can
use an ampersand ([`&`](../api/#org.mashupbots.socko.routes.$amp$)). 

For example, if you wish to match a path of `/record/1` and a query string of `action=save`,
you can use 

    object ActionQueryStringField extends QueryStringName("action")
    
    val r = Routes({
      case PathSegments("record" :: id :: Nil) & ActionQueryStringField(actionValue) => {
        ...
      }
    })





## Step 3. Start/Stop Web Server <a class="blank" id="Step3">&nbsp;</a>

To start you web server, you only need to instance the {{ page.WebServerClass }} class and 
call `start()` passing in your configuration and routes.  When you wish to stop the web 
server, call `stop()`.

    def main(args: Array[String]) {
      val webServer = new WebServer(WebServerConfig(), routes)
      webServer.start()
  
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run { webServer.stop() }
      })
    
      System.out.println("Open your browser and navigate to http://localhost:8888")
    }
  
This example uses the default configuration which starts the web server at `localhost` bound on
port `8888`.  To customise, refer to [Configuration](#Configuration).




## Configuration <a class="blank" id="Configuration">&nbsp;</a>

A web server's configuration is defined in {{ page.WebServerConfigClass }}.

Web server configuration is immutable. To change the configuration, a new {{ page.WebServerClass }} class
must be instanced with the new configuration and started.

Common settings are:

 - `serverName`

   Human friendly name of this server. Defaults to `WebServer`.
    
 - `hostname`
 
   Hostname or IP address to bind. `0.0.0.0` will bind to all addresses. You can also specify comma 
   separated hostnames/ip address like `localhost,192.168.1.1`. Defaults to `localhost`.
   
 - `port`
 
   IP port number to bind to. Defaults to `8888`.
   
 - `webLog`
   
   Web server activity log.
   
 - `ssl`
 
   Optional SSL configuration. Default is `None`.
   
 - `http`
 
   Optional HTTP request settings.

Refer to the api documentation of {{ page.WebServerConfigClass }} for all settings.

Configuration can be changed in [code](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/config/CodedConfigApp.scala)
or in the project's [Akka configuration file](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/config/AkkaConfigApp.scala).

For example, to change the port to `7777` in code:

    val webServer = new WebServer(WebServerConfig(port=7777), routes)


To change the port to `9999` in your Akka configuration file, first define an object to load the settings
from `application.conf`. Note the setting will be named `akka-config-example`.

    object MyWebServerConfig extends ExtensionId[WebServerConfig] with ExtensionIdProvider {
      override def lookup = MyWebServerConfig
      override def createExtension(system: ExtendedActorSystem) =
        new WebServerConfig(system.settings.config, "akka-config-example")
    }

Then, start the actor system and load the configuration from that system.

    val actorSystem = ActorSystem("AkkaConfigActorSystem")
    val myWebServerConfig = MyWebServerConfig(actorSystem)
    
Lastly, add the following our `application.conf`

    akka-config-example {
        port=9999
    }

A complete example `application.conf` can be found in {{ page.WebServerConfigClass }}.




## Web Logs <a class="blank" id="WebLogs">&nbsp;</a>

Socko supports 3 web log formats:

 - [Common](http://en.wikipedia.org/wiki/Common_Log_Format) - Apache Common format
 - [Combined](http://httpd.apache.org/docs/current/logs.html) - Apache Combined format
 - [Extended](http://www.w3.org/TR/WD-logfile.html) - Extended format

Examples:

    ** COMMON **
    216.67.1.91 - leon [01/Jul/2002:12:11:52 +0000] "GET /index.html HTTP/1.1" 200 431 "http://www.loganalyzer.net/"
    
    ** COMBINED **
    216.67.1.91 - leon [01/Jul/2002:12:11:52 +0000] "GET /index.html HTTP/1.1" 200 431 "http://www.loganalyzer.net/" "Mozilla/4.05 [en] (WinNT; I)"
    
    ** EXTENDED **
    #Software: Socko
    #Version: 1.0
    #Date: 2002-05-02 17:42:15
    #Fields: date time c-ip cs-username s-ip s-port cs-method cs-uri-stem cs-uri-query sc-status sc-bytes cs-bytes time-taken cs(User-Agent) cs(Referrer)
    2002-05-24 20:18:01 172.224.24.114 - 206.73.118.24 80 GET /Default.htm - 200 7930 248 31 Mozilla/4.0+(compatible;+MSIE+5.01;+Windows+2000+Server) http://64.224.24.114/

### Turning On Web Logs

By default, web logs are turned **OFF**.

To turn web logs on, add the following `web-log` section to your `application.conf`
 
    akka-config-example {
      server-name=AkkaConfigExample
      hostname=localhost
      port=9000
      
      # Optional web log. If not supplied, web server activity logging is turned off.
      web-log {
      
        # Optional Web log format: Common (Default) or Extended 
        format = Common
        
        # Optional asynchronous log queue size. Defaults to 512 events in the queue 
        # before new events are discarded.
        buffer-size = 512
      }
    }
    
You can also turn it on programmatically as illustrated in the [route example app](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/routes/RouteApp.scala).

    // Turn on web logs
    // Web logs will be written to the logger. You can control output via logback.xml.
    val config = WebServerConfig(webLog = Some(WebLogConfig()))
    val webServer = new WebServer(config, routes)
    
When turned on, the default behaviour is to write web logs to your installed logger using {{ page.DefaultWebLogWriterClass }}. 
You can control where these logs are written by configuring your logger. For example, if you are using 
[Logback](http://logback.qos.ch/), you can write to a daily rolling file by changing `logback.xml` to include:

    <configuration>
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
          <pattern>%d{HH:mm:ss.SSS} [%thread] [%X{sourceThread}] %-5level %logger{36} %X{akkaSource} - %msg%n</pattern>
        </encoder>
      </appender>

      <appender name="WEBLOG" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logFile.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
          <!-- daily rollover -->
          <fileNamePattern>logFile.%d{yyyy-MM-dd}.log</fileNamePattern>

          <!-- keep 30 days' worth of history -->
          <maxHistory>30</maxHistory>
        </rollingPolicy>

        <encoder>
          <pattern>%msg%n</pattern>
        </encoder>
      </appender> 
  
      <logger name="org.mashupbots.socko.utils.DefaultWebLogWriter" level="info">
        <appender-ref ref="WEBLOG" />
      </logger>

      <root level="info">
        <appender-ref ref="STDOUT" />
      </root>
    </configuration>

### Recording Web Logs Events

Web log events can be recorded using the processing context.

 - {{ page.HttpRequestContextClass }}
 
   Web logs events are automatically recorded for you when you call `response.write()` or `response.redirect()` methods.

   These methods assume a non-chunked response.
   
   If you wish to return a chunked response (for example, our `StaticFileProcessor`), you will have to record the web
   log event yourself using the `writeWebLog()` method.

 - {{ page.HttpChunkContextClass }}

   As per {{ page.HttpRequestContextClass }}.
   
 - {{ page.WebSocketFrameContextClass }}
 
   Web log events are **NOT** automatically recorded for you. This is becasue web sockets do not strictly follow 
   the request/response structure of HTTP. For example, in a chat server, a broadcast message will not have a request
   frame.
   
   If you wish to record a web log event, you can use the `writeWebLog()` method. The method, uri and other details
   of the event to be recorded is arbitrarily set by you.
   
 - {{ page.WebSocketHandshakeContextClass }}
   
   Web log events are automatically recorded for you.
   

### Custom Web Log Output

If you prefer to use your own method and/or format of writing web logs, you can specify a custom {{ page.WebLogQueueClass }}
implementation as follows:

    val config = WebServerConfig()
    val routes = ...
    val myWebLogQueue = Some(new MyCustomWebLogQueue())
    val webServer = new WebServer(config, routes, myWebLogQueue)

You can then use the {{ page.DefaultWebLogWriterClass }} or your own customer writer to dequeue and write out the web log events.
For optimal performance, we strongly recommend you dequeue and write web logs in a separate thread.

    // Using the default web log writer
    val defaultWriterThread = new Thread(new DefaultWebLogWriter(myWebLogQueue, WebLogFormat.Extended))
    defaultWriterThread.start()
    
    // Using your own writer
    val myWriterThread = new Thread(new MyCustomWebLogWriter(myWebLogQueue))
    myWriterThread.start()



