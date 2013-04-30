---
layout: docs
title: Socko User Guide - Serving Dynamic HTTP Content

SockoEventClass: <code><a href="../api/#org.mashupbots.socko.events.SockoEvent">SockoEvent</a></code>
HttpRequestEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpRequestEvent">HttpRequestEvent</a></code>
HttpChunkEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpChunkEvent">HttpChunkEvent</a></code>
WebSocketFrameEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketFrameEvent">WebSocketFrameEvent</a></code>
WebSocketHandshakeEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketHandshakeEvent">WebSocketHandshakeEvent</a></code>
WebServerClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServer">WebServer</a></code>
WebServerConfigClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServerConfig">WebServerConfig</a></code>
WebSocketBroadcasterClass: <code><a href="../api/#org.mashupbots.socko.handler.WebSocketBroadcaster">WebSocketBroadcaster</a></code>
---
# Socko User Guide - Serving Dynamic HTTP Content

## Introduction

 - [Parsing Data](#ParsingData)
   - [Query Strings](#QueryStrings)
   - [Post Form Data](#PostFormData)
   - [File Uploads](#FileUploads)
 - [RESTful Web Services](#Rest)
   - [Definition](dynamic-content.html#RestDefinition)
   - [Registration](dynamic-content.html#RestRegistration)
   - [Configuration](dynamic-content.html#RestConfiguration)
   - [Swagger](dynamic-content.html#RestSwagger)
 - [Web Sockets](#WebSockets)


## Parsing Data <a class="blank" id="ParsingData">&nbsp;</a>

### Parsing Query Strings <a class="blank" id="QueryStrings">&nbsp;</a>

You can access query string parameters using {{ page.HttpRequestEventClass }}.

{% highlight scala %}
  // For mypath?a=1&b=2
  val qsMap = event.endPoint.queryStringMap
  assert (qsMap("a") == "1")
{% endhighlight %}

See the [query string and post data example](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/querystring_post)
for more details.

### Parsing Post Form Data <a class="blank" id="PostFormData">&nbsp;</a>

If you do not have to support file uploads and your post form data content type is `application/x-www-form-urlencoded data`,
you can als access form data using {{ page.HttpRequestEventClass }}.

{% highlight scala %}
  // For firstName=jim&lastName=low
  val formDataMap = event.request.content.toFormDataMap
  assert (formDataMap("firstName") == "jim")
{% endhighlight %}

See the [query string and post data example](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/querystring_post)
for more details.

### Parsing File Uploads <a class="blank" id="FileUploads">&nbsp;</a>

If you intend to support file uploads, you need to use Netty's [HttpPostRequestDecoder](http://static.netty.io/3.6/api/org/jboss/netty/handler/codec/http/multipart/HttpPostRequestDecoder.html).

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




## RESTful Web Services <a class="blank" id="Rest">&nbsp;</a>

TO DO......

Add the module to your project


## Web Sockets <a class="blank" id="WebSockets">&nbsp;</a>

For a detailed discussions on how web sockets work, refer to [RFC 6455](http://tools.ietf.org/html/rfc6455)

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

If you wish to push or broadcast messages to a group of web socket connections, use {{ page.WebSocketBroadcasterClass }}.
See the example web socket [ChatApp](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/websocket/ChatApp.scala) for usage.




