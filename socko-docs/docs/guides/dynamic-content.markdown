---
layout: docs
title: Socko User Guide - Serving Dynamic HTTP Content

SockoEventClass: <code><a href="../api/#org.mashupbots.socko.events.SockoEvent">SockoEvent</a></code>
HttpRequestEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpRequestEvent">HttpRequestEvent</a></code>
HttpChunkEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpChunkEvent">HttpChunkEvent</a></code>
RestHandlerClass: <code><a href="../api/#org.mashupbots.socko.rest.RestHandler">RestHandler</a></code>
RestModelMetaDataClass: <code><a href="../api/#org.mashupbots.socko.rest.RestModelMetaData">RestModelMetaData</a></code>
RestPropertyMetaDataClass: <code><a href="../api/#org.mashupbots.socko.rest.RestPropertyMetaData">RestPropertyMetaData</a></code>
RestRegistrationClass: <code><a href="../api/#org.mashupbots.socko.rest.RestRegistration">RestRegistration</a></code>
RestRequestClass: <code><a href="../api/#org.mashupbots.socko.rest.RestRequest">RestRequest</a></code>
RestResponseClass: <code><a href="../api/#org.mashupbots.socko.rest.RestResponse">RestResponse</a></code>
RestRegistryClass: <code><a href="../api/#org.mashupbots.socko.rest.RestRegistry">RestRegistry</a></code>
RestConfigClass: <code><a href="../api/#org.mashupbots.socko.rest.RestConfig">RestConfig</a></code>
WebSocketFrameEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketFrameEvent">WebSocketFrameEvent</a></code>
WebSocketHandshakeEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketHandshakeEvent">WebSocketHandshakeEvent</a></code>
WebServerClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServer">WebServer</a></code>
WebServerConfigClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServerConfig">WebServerConfig</a></code>
---
# Socko User Guide - Serving Dynamic HTTP Content

## Introduction

 - [Parsing Data](#ParsingData)
   - [Query Strings](#QueryStrings)
   - [Post Form Data](#PostFormData)
   - [File Uploads](#FileUploads)
 - [RESTful Web Services](#Rest)
   - [Step 1. Installation](dynamic-content.html#RestInstallation)
   - [Step 2. Implement your Data Model](dynamic-content.html#RestDataModel)
   - [Step 3. Implement your Business Logic Actors](dynamic-content.html#RestBusinessLogic)
   - [Step 4. Register your REST Operation](dynamic-content.html#RestRegistration)
   - [Step 5. Using the REST Handler](dynamic-content.html#RestHandler)
   - [Configuration](dynamic-content.html#RestConfiguration)
   - [Supported Data Types](dynamic-content.html#RestDataType)
 - [Web Sockets](#WebSockets)
   - [Callbacks](dynamic-content.html#WebSocketCallbacks)
   - [Pushing Data](dynamic-content.html#WebSocketPush)
   - [Closing Web Socket Connections](dynamic-content.html#WebSocketClosed)

## Parsing Data <a class="blank" id="ParsingData"></a>

### Parsing Query Strings <a class="blank" id="QueryStrings"></a>

You can access query string parameters using {{ page.HttpRequestEventClass }}.

{% highlight scala %}
  // For mypath?a=1&b=2
  val qsMap = event.endPoint.queryStringMap
  assert (qsMap("a") == "1")
{% endhighlight %}

See the [query string and post data example](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/querystring_post)
for more details.

### Parsing Post Form Data <a class="blank" id="PostFormData"></a>

If you do not have to support file uploads and your post form data content type is `application/x-www-form-urlencoded data`,
you can access the form data using {{ page.HttpRequestEventClass }}.

{% highlight scala %}
  // For firstName=jim&lastName=low
  val formDataMap = event.request.content.toFormDataMap
  assert (formDataMap("firstName") == "jim")
{% endhighlight %}

See the [query string and post data example](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/querystring_post)
for more details.

### Parsing File Uploads <a class="blank" id="FileUploads"></a>

If you intend to support file uploads, you need to use Netty's [HttpPostRequestDecoder](http://netty.io/4.0/api/io/netty/handler/codec/http/multipart/HttpPostRequestDecoder.html).

The following example extracts the `description` field as well as a file that was posted.

{% highlight scala %}
  //
  // The following form has a file upload input named "fileUpload" and a description
  // field named "fileDescription".
  //
  // ------WebKitFormBoundaryThBHDfQBdTlMy3sK
  // Content-Disposition: form-data; name="fileUpload"; filename="myfile.txt"
  // Content-Type: text/plain
  // 
  // file contents
  // ------WebKitFormBoundaryThBHDfQBdTlMy3sK
  // Content-Disposition: form-data; name="fileDescription"
  // 
  // this is my file upload
  // ------WebKitFormBoundaryThBHDfQBdTlMy3sK--
  //

  val decoder = new HttpPostRequestDecoder(HttpDataFactory.value, event.nettyHttpRequest)
  val descriptionField = decoder.getBodyHttpData("fileDescription").asInstanceOf[Attribute]
  val fileField = decoder.getBodyHttpData("fileUpload").asInstanceOf[FileUpload]
  val destFile = new File(msg.saveDir, fileField.getFilename)
{% endhighlight %}

See the [file upload example](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/fileupload)
for more details.




## RESTful Web Services <a class="blank" id="Rest"></a>

Socko's {{ page.RestHandlerClass }} class provides a quick way to expose your Akka actors as REST end points. You 
will be able to invoke your Akka actors using Javascript in a web browser app and from other HTTP clients.

It also natively supports [Swagger](https://developers.helloreverb.com/swagger/).  With Swagger, you will be able to 
provide your users with your API documentation straight from your code.

The current limitations of Socko's {{ page.RestHandlerClass }} are:
 - Only JSON is supported. XML is not supported.
 - Only HTTP is supported. Web Socket is not supported.
 - This module relies on Scala reflection. Reflection is tagged as experimental in Scala 2.10. There is also 
   an issue with [thread safety](http://docs.scala-lang.org/overviews/reflection/thread-safety.html). For unit tests
   to work, you have to execute them **serially** (in `sbt` set `parallelExecution in Test := false`). Also,
   instancing {{ page.RestRegistryClass }} in Step #5 must be performed in a single thead.

To use the {{ page.RestHandlerClass }} class, follow the 5 steps below. The example code can be found in the 
[Pet Shop example REST app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/rest).


### Step 1. Installation <a class="blank" id="RestInstallation"></a>

In order to use the Socko RESTful Web Service framework, you need to add an extra dependency.

Add the following to your `build.sbt`.  Replace `X.Y.Z` with the version number.

    libraryDependencies += "org.mashupbots.socko" %% "socko-rest" % "X.Y.Z"


### Step 2. Implement your Data Model <a class="blank" id="RestDefinition"></a>

Define your model as case classes.

{% highlight scala %}
  case class Tag(id: Long, name: String)
  case class Category(id: Long, name: String)
  case class Pet(id: Long, category: Category, name: String, photoUrls: List[String], tags: List[Tag], status: String)
{% endhighlight %}

You can specify additional information to describe the properties in your data model using {{ page.RestModelMetaDataClass }}. 
For example, to add more information about a `Pet`, create a companion `Pet` object that extends {{ page.RestModelMetaDataClass }}.
Then, provide additional meta data using {{ page.RestPropertyMetaDataClass }}.

{% highlight scala %}
  object Pet extends RestModelMetaData {
    val modelProperties = Seq(
      RestPropertyMetaData("name", "Name of the pet"),
      RestPropertyMetaData("status", "pet status in the store", Some(AllowableValuesList(List("available", "pending", "sold")))))
  }
{% endhighlight %}


### Step 3. Implement your Business Logic Actors <a class="blank" id="RestBusinessLogic"></a>

Socko {{ page.RestHandlerClass }} uses a Request/Response model.

Incoming request data is deserialized into {{ page.RestRequestClass }}. Similarly, an outgoing {{ page.RestResponseClass }} is 
serialized into response data.

The following is an example implementation of a REST operation that returns a `Pet` given the pet's `id` as input.

{% highlight scala %}
  case class GetPetRequest(context: RestRequestContext, petId: Long) extends RestRequest
  case class GetPetResponse(context: RestResponseContext, pet: Option[Pet]) extends RestResponse

  class PetProcessor() extends Actor with akka.actor.ActorLogging {
    def receive = {
      case req: GetPetRequest =>
        val pet = PetData.getPetById(req.petId)
        val response = if (pet != null) {
          GetPetResponse(req.context.responseContext, Some(pet))
        } else {
          GetPetResponse(req.context.responseContext(404), None)
        }
        sender ! response
        context.stop(self)
    }
  }
{% endhighlight %}

Your request and response must extend from {{ page.RestRequestClass }} and {{ page.RestResponseClass }} respectively 
as illustrated in `GetPetRequest` and `GetPetResponse`. Your request and resposne **must** declare `context` as the 
first parameter. Subsequement parameters can contain primitives and your data model classes.


### Step 4. Register your REST Operation <a class="blank" id="RestRegistration"></a>

In order to notify {{ page.RestHandlerClass }} to the existance of your actor, you need to regsiter it.

Registering involves creating a Scala `object` that extends {{ page.RestRegistrationClass }}.

At minimum, you must provide the method, path, bindings for your request parameters and the actor that you wish to use to 
process incoming requests.

{% highlight scala %}
  object GetPetRegistration extends RestRegistration {
    val method = Method.GET
    val path = "/pet/{petId}"
    val requestParams = Seq(PathParam("petId", "ID of pet that needs to be fetched"))
    def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = actorSystem.actorOf(Props[PetProcessor])
  }
{% endhighlight %}

You can also specify additional information to describe your REST operation.

{% highlight scala %}
  object GetPetRegistration extends RestRegistration {
    val method = Method.GET
    val path = "/pet/{petId}"
    val requestParams = Seq(PathParam("petId", "ID of pet that needs to be fetched"))
    def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef = actorSystem.actorOf(Props[PetProcessor])
    override val name = "getPetById"
    override val description = "Find pet by ID"
    override val notes = "Returns a pet based on ID"
    override val errors = Seq(Error(400, "Invalid ID supplied"), Error(404, "Pet not found"))
  }
{% endhighlight %}

A key part of registration is defining how you return your actor using `processorActor()`.  In the [Pet Shop example REST app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/rest),
we have provided 3 example scenarios:

 - `Pet` uses a single actor for all operations. The actor is instanced and terminated for each request.
 - `Store` uses an actor per operation. The actors are instanced and terminated for each request.
 - `User` uses a single actor for all operation. A pool of actors is instanced under a router at startup. The actors are 
    **not** terminated; rather they are reused.


### Step 5. Using the REST Handler <a class="blank" id="RestHandler"></a>

In order to use {{ page.RestHandlerClass }}, you must first instance {{ page.RestRegistryClass }}. The registry 
will search the code base for your {{ page.RestRegistrationClass }}s.

Using an Akka router, you can then instance {{ page.RestHandlerClass }} with the {{ page.RestRegistryClass }}.

Finally, add the {{ page.RestHandlerClass }} to your route and start Socko web server.

The following example illustrates:

{% highlight scala %}
  //
  // STEP #1 - Define Actors and Start Akka
  //
  val actorConfig = """
	akka {
	  event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
	  loglevel=DEBUG
	  actor {
	    deployment {
	      /rest-router {
	        router = round-robin-pool
	        nr-of-instances = 5
	      }
	    }
	  }
	}"""

  val actorSystem = ActorSystem("RestExampleActorSystem", ConfigFactory.parseString(actorConfig))

  val restRegistry = RestRegistry("org.mashupbots.socko.examples.rest",
    RestConfig("1.0", "http://localhost:8888/api", reportRuntimeException = ReportRuntimeException.All))

  val restRouter = actorSystem.actorOf(Props(new RestHandler(restRegistry)).withRouter(FromConfig()), "rest-router")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case HttpRequest(request) => request match {
      case PathSegments("swagger-ui" :: relativePath) => {
        // Serve the static swagger-ui content from resources
        staticContentHandlerRouter ! new StaticResourceRequest(request, relativePath.mkString("swaggerui/", "/", ""))
      }
      case PathSegments("api" :: relativePath) => {
        // REST API - just pass the request to the handler for processing
        restRouter ! request
      }
      case GET(Path("/favicon.ico")) => {
        request.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }
  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    // Start web server
    val config = WebServerConfig(webLog = Some(WebLogConfig()))
    val webServer = new WebServer(config, routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run {
        webServer.stop()
      }
    })
    webServer.start()

    System.out.println("Open your browser and navigate to http://localhost:8888/swagger-ui/index.html")
  }
{% endhighlight %}


### Configuration <a class="blank" id="RestConfiguration"></a>

The {{ page.RestConfigClass }} class is used to configure the REST handler. It is specified when instancing 
{{ page.RestRegistryClass }}.

Required settings are:

 - `apiVersion`

   Your API version.
    
 - `rootApiUrl`
 
   Root path to your API with the scheme, domain and port. This is the path as seen by the end user and **not** from 
   on the local server. It needs to take into consideration the path to {{ page.RestHandlerClass }} as set in your route. 
   In the above example, the route is set to `api` so `rootApiUrl` must be set to `http://yourdomain.com/api`.

Like other settings, you can set the values programmatically or via an external Akka configuration file. See
{{ page.RestConfigClass }} for more details. 


### Supported Data Types <a class="blank" id="RestDataType"></a>

The supported data types aligns with [swagger](https://github.com/wordnik/swagger-core/wiki/Datatypes).

 - `String`
 - `Int`
 - `Boolean`
 - `Byte`
 - `Short`
 - `Long`
 - `Double`
 - `Float`
 - `Date`
 - `AnyRef` (class)

`Option`, `Seq` or `Array` of the above are also supported.




## Web Sockets <a class="blank" id="WebSockets"></a>

For a detailed discussion on how web sockets work, refer to [RFC 6455](http://tools.ietf.org/html/rfc6455).

Prior to a web socket connection being established, a web socket handshake must take place. In Socko, a
{{ page.WebSocketHandshakeEventClass }} is fired when a handshake is required.

After a successful handshake, {{ page.WebSocketFrameEventClass }} is fired when a web socket text or binary
frame is received.

The following route from our web socket example app illustrates:

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

Note that for a web socket handshake, you only need to call `wsHandshake.authorize()` in order to approve the connection.
This is a security measure to make sure that web sockets can only be established at your specified end points.
Dispatching to an actor is not required and not recommended.

You can also specify subprotocols and maximum frame size with authorization. If not specified, the default is no 
subprotocol support and a maximum frame size of 100K.

    // Only support chat and superchat subprotocols and max frame size of 1000 bytes
    wsHandshake.authorize("chat, superchat", 1000)

See the example web socket [ChatApp](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/websocket/ChatApp.scala) for usage.


### Callbacks <a class="blank" id="WebSocketCallbacks"></a>

As part of `authorize()`, you are able to supply callback functions:

 - `onComplete`: called when the handshake completes
 - `onClose`: called when the web socket connection is closed

For both functions, a unique identifier for the web socket connection is passed as a parameter.

{% highlight scala %}
    def onWebSocketHandshakeComplete(webSocketId: String) {
      System.out.println(s"Web Socket $webSocketId connected")
    }

    def onWebSocketClose(webSocketId: String) {
      System.out.println(s"Web Socket $webSocketId closed")
    }

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
          // Pass in callback functions upon authorization
          wsHandshake.authorize(
            onComplete = Some(onWebSocketHandshakeComplete),
            onClose = Some(onWebSocketClose))
        }
      }
    
      case WebSocketFrame(wsFrame) => {
        // Once handshaking has taken place, we can now process frames sent from the client
        actorSystem.actorOf(Props[WebSocketHandler]) ! wsFrame
      }
    
    })

{% endhighlight %}


### Pushing Data <a class="blank" id="WebSocketPush"></a>

After a web socket connection is authorized, it is added to the web server object's `webSocketConnections`. Using this, you can push data to one or more web socket clients.

Each web socket connection has a unique identifier that you must store if you wish to push data to a specific connection.  The identifier is provided in the `WebSocketHandshake` event
as well as to the `onComplete` and `onClose` callback functions.

{% highlight scala %}
    var myWebSocketId = ""

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
          // Store web socket ID for future use
          myWebSocketId = wsHandshake.webSocketId     

          // Authorize
          wsHandshake.authorize()
        }
      }
        
    })
{% endhighlight %}


To push a message:

{% highlight scala %}
    // Broadcast to all connections
    MyApp.webServer.webSocketConnections.writeText("Broadcast message to all web socket connections...")

    // Send to a specific connection
    MyApp.webServer.webSocketConnections.writeText("Hello", myWebSocketId)
{% endhighlight %}


An alternative way to push messages is to wrap the `webServer` instance inside an actor. You can push messages by sending the message to the
actor.

{% highlight scala %}
    class MyActor(webServer: WebServer) extends Actor {
      val log = Logging(context.system, this)
      def receive = {
        case s: String ⇒ webServer.webSocketConnections.writeText(s)
        case _      ⇒ log.info("received unknown message")
      }
    }
{% endhighlight %}


### Closing Web Socket Connections <a class="blank" id="WebSocketClosed"></a>

You can check connectivity and close web socket connections:

{% highlight scala %}
    // Close all connections
    MyApp.webServer.webSocketConnections.closeAll()

    // Close a specific web socket connection
    if (MyApp.webServer.webSocketConnections.isConnected(myWebSocketId)) {
      MyApp.webServer.webSocketConnections.close(myWebSocketId)
    }
{% endhighlight %}


