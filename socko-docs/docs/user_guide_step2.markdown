---
layout: docs
title: Socko User Guide
---
# Socko User Guide

## Step 2. Define Routes. <a class="blank" id="Step2">&nbsp;</a>

Routes allows to control how Socko dispatches incoming requests to your actors.

Routes are implemented in Socko as a partial function.  The following example illustrates 
matching HTTP GET requests and dispatching the request to a `HelloProcessor` actor:

    val routes = Routes({
      case GET(request) => {
        actorSystem.actorOf(Props[HelloProcessor]) ! request
      }
    })
  

Socko has predefined extractors to assist with matching requests.

 - [Processing Context Extractors](#ProcessingContextExtractors)
 - [Method Extractors](#MethodExtractors)
 - [Host Extractors](#HostExtractors)
 - [Path Extractors](#PathExtractors)
 - [QueryString Extractors](#QueryStringExtractors)
 - [Concatenating Extractors](#ConcatenatingExtractors)

### Processing Context Extractors <a class="blank" id="ProcessingContextExtractors">&nbsp;</a>

These extactors allows you to match different types of [`ProcessingContext`](../api/#org.mashupbots.socko.context.ProcessingContext).

 - [`HttpRequest`](../api/#org.mashupbots.socko.routes.HttpRequest$) matches [`HttpRequestProcessingContext`](../api/#org.mashupbots.socko.context.HttpRequestProcessingContext).
 - [`HttpChunk`](../api/#org.mashupbots.socko.routes.HttpChunk$) matches [`HttpChunkProcessingContext`](../api/#org.mashupbots.socko.context.HttpChunkProcessingContext).
 - [`WebSocketFrame`](../api/#org.mashupbots.socko.routes.WebSocketFrame$) matches [`WsFrameProcessingContext`](../api/#org.mashupbots.socko.context.WsFrameProcessingContext).
 - [`WebSocketHandshake`](../api/#org.mashupbots.socko.routes.WebSocketHandshake$) matches [`WsHandshakeProcessingContext`](../api/#org.mashupbots.socko.context.WsHandshakeProcessingContext).

We often use processing context extractors for "top level" matching.

The following code taken form our WebSocket example app illustrates usage 

    val routes = Routes({
    
      case HttpRequest(httpRequest) => httpRequest match {
        case GET(Path("/html")) => {
          // Return HTML page to establish web socket
          actorSystem.actorOf(Props[WebSocketProcessor]) ! httpRequest
        }
        case Path("/favicon.ico") => {
          // If favicon.ico, just return a 404 because we don't have that file
          httpRequest.writeErrorResponse(HttpResponseStatus.NOT_FOUND, false, "")
        }
      }
      
      case WebSocketHandshake(wsHandshake) => wsHandshake match {
        case Path("/websocket/") => {
          // For WebSocket processing, we first have to authorize the handshake by setting the "isAllowed" property.
          // This is a security measure to make sure that web sockets can only be established at your specified end points.
          wsHandshake.isAllowed = true
        }
      }
    
      case WebSocketFrame(wsFrame) => {
        // Once handshaking has taken place, we can now process frames sent from the client
        actorSystem.actorOf(Props[WebSocketProcessor]) ! wsFrame
      }
    
    })

Note that for a web socket handshake, you only need to set the context's `isAllowed` property to true.
Dispatchig to an actor is not required or recommended.

### Method Extractors <a class="blank" id="MethodExtractors">&nbsp;</a>



### Host Extractors <a class="blank" id="HostExtractors">&nbsp;</a>

Host extractors matches the host name received in the request.

For HTTP Requests, the host name is the value specified in the `HOST` header variable. For HTTP Chunks and 
Web Socket handshakes and frames, the host is that of the associated initial HTTP Request.

**[`Host`](../api/#org.mashupbots.socko.routes.Host$)**

Performs an exact match on the specified hostname.

The following example will match `www.sockoweb.org` but not: `www1.sockoweb.org`, `sockoweb.com` or `sockoweb.org`.

    val r = Routes({
      case Host("www.sockoweb.org") => {
        ...
      }
    })


**[`HostSegments`](../api/#org.mashupbots.socko.routes.HostSegments$)**

Performs matching and variable binding on segments of a hostname. Each segement is assumed to be delimited
by a period.

For example:

    val r = Routes({
      // Matches server1.sockoweb.org
      case Host(HostSegments(server :: "sockoweb" :: "org" :: Nil)) => {
        ...
      }
    })

This will match any hostnames that have 3 segements and the last 2 segements being `sockoweb.org`. 
The first segment will be bound to a variable called `server.` 

This will match `www.sockoweb.org` and the `server` variable have a value of `www`.

This will NOT match `www.sockoweb.com` or `sockweb.org`.

 
**[`HostRegex`](../api/#org.mashupbots.socko.routes.HostRegex$)**

Matches the hostname based on a regular expression pattern.

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


### Path Extractors <a class="blank" id="PathExtractors">&nbsp;</a>

Path extractors matches the path received in the request.

For HTTP Requests, the path is the extracted from the 1st line without any querystring. 
For HTTP Chunks and Web Socket handshakes and frames, the path is that of the associated initial HTTP Request.

**[`Path`](../api/#org.mashupbots.socko.routes.Path$)**

Performs an exact match on the specified path.

The following example will match `folderX` but not: `/folderx`, `/folderX/` or `/TheFolderX`.

    val r = Routes({
      case Path("/folderX") => {
        ...
      }
    })


**[`PathSegments`](../api/#org.mashupbots.socko.routes.PathSegments$)**

Performs matching and variable binding on segments of a path. Each segement is assumed to be delimited
by a slash.

For example:

    val r = Routes({
      // Matches /record/1
      case Path(PathSegments("record" :: id :: Nil)) => {
        ...
      }
    })

This will match any paths that have 2 segements and the first segement being `record`. The second segment will
be bound to a variable called `id.` 

This will match `/record/1` and `id` will be set to `1`.

This will NOT match `/record` or `/record/1/2`.

 
**[`PathRegex`](../api/#org.mashupbots.socko.routes.PathRegex$)**

Matches the path based on a regular expression pattern.

For example, to match `/path/to/file`, first define your regular expression as an object and then use it
in your route.

    object MyPathRegex extends PathRegex("""/path/([a-z0-9]+)/([a-z0-9]+)""".r)
    
    val r = Routes({
      // Matches /path/to/file
      case case MyPathRegex(m) => {
        assert(m.group(1) == "to")
        assert(m.group(2) == "file")
        ...
      }
    })


### QueryString Extractors <a class="blank" id="QueryStringExtractors">&nbsp;</a>

### Concatenating Extractors <a class="blank" id="ConcatenatingExtractors">&nbsp;</a>



