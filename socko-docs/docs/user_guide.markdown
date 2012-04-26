---
layout: docs
title: Socko User Guide
---
# Socko User Guide

Socko is an embedded web server.  It is designed to be used within your application.

You can add Socko to your application in 3 steps.


## Step 1. Define Actors and Start Akka.

Socko assumes that you have your business rules implemented as Akka v2 Actors.

### Processing Context
In order to read requests and writes responses, your actors must use Socko's `ProcessingContext`.

There are 2 ways to achieve this:

1. You can write a facade actor to specifically handle Socko `ProcessingContext`.

2. You can change your actors to be Socko `ProcessingContext` aware by adding a `ProcessingContext` 
   property to messages that it receives.

There are 4 types of `ProcessingContext`:

1. `HttpRequestProcessingContext` will be sent to your actor for a HTTP Request is received

2. `HttpChunkProcessingContext` will be sent to your actor for a HTTP Chunk is received

3. `WsFrameProcessingContext` will be sent to your actor when a Web Socket Frame is received

4. `WsHandshakeProcessingContext` is used for Web Socket handshaking within your Route (see Step #2).
   It will **not** be sent to your actor.


### Akka Dispatchers (Thread Pool)

Akka [dispatchers](http://doc.akka.io/docs/akka/2.0.1/scala/dispatchers.html) controls how your Akka 
actors processes messages.

The default dispatcher should be used for most instances.

However, if your actors have blocking opereations like database read/write or file system read/write, 
we recommend that you use a dispatcher driven by the `tread-pool-executor`.  For example, the 
`PinnedDispatcher` is used in the Socko's file upload 
[example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/fileupload).

Isolating blocking operations to a thread pool means that other actors can continue processing without
waiting for your actor's blocking operation to finish.

## Step 2. Define Routes.




## Step 3. Start/Stop Web Server.





