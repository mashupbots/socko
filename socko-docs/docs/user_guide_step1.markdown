---
layout: docs
title: Socko User Guide
---
# Socko User Guide

## Step 1. Define Actors and Start Akka. <a class="blank" id="Step1">&nbsp;</a>

Socko assumes that you have your business rules implemented as Akka v2 Actors.

Requests received by Socko will be passed to your actors within a Socko 
[`ProcessingContext`](../api/#org.mashupbots.socko.context.ProcessingContext).  Your actors use
[`ProcessingContext`](../api/#org.mashupbots.socko.context.ProcessingContext) to read the request and
write the response.

In the following `HelloApp` example, we have defined an actor called `HelloProcessor` and started an Akka
system called `HelloExampleActorSystem`.  The `HttpRequestProcessingContext` is used by `HelloProcessor`
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
        case request: HttpRequestProcessingContext =>
          request.writeResponse("Hello from Socko (" + new Date().toString + ")")
          context.stop(self)
      }
    }
    
For maximum scalability and performance, you will need to carefully choose your Akka dispatchers.
The default dispatcher is optimized for non blocking code. If your code blocks, for example 
database, file system and network IO, then it is best to configure Akka to use dispatchers based
on thread pools.

### Processing Context
In order to read requests and writes responses, your actors must use Socko's `ProcessingContext`.

Two ways to achieve this are:

1. You can change your actors to be Socko `ProcessingContext` aware by adding a `ProcessingContext` 
   property to messages that it receives - as per above example.

2. You can write a facade actor to specifically handle Socko `ProcessingContext`. Your facade can
   read the request from the `ProcessingContext` in order to create messages to pass to your actors
   for processing. Your facade should also setup Akka [futures](http://doc.akka.io/docs/akka/2.0.1/scala/futures.html)
   so that when processing is completed, the `ProcessingContext` can be used to write your response.

There are 4 types of `ProcessingContext`:

1. [`HttpRequestProcessingContext`](../api/#org.mashupbots.socko.context.HttpRequestProcessingContext) 
   will be sent to your actor when a HTTP Request is received.

2. [`HttpChunkProcessingContext`](../api/#org.mashupbots.socko.context.HttpChunkProcessingContext) 
   will be sent to your actor when a HTTP Chunk is received. This is only applicable if you turn off 
   [chunk aggregation](user_guide_configuration.html).

3. [`WsFrameProcessingContext`](../api/#org.mashupbots.socko.context.WsFrameProcessingContext) 
   will be sent to your actor when a Web Socket Frame is received.

4. [`WsHandshakeProcessingContext`](../api/#org.mashupbots.socko.context.WsHandshakeProcessingContext) 
   is used for Web Socket handshaking within your [Route](#user_guide_step2.html).
   It should **not** be sent to your actor.

Note that the `ProcessingContext` must only be used by by local actors.

### Akka Dispatchers (Thread Pool)

Akka [dispatchers](http://doc.akka.io/docs/akka/2.0.1/scala/dispatchers.html) controls how your Akka 
actors processes messages.

The default dispatcher is optimized for non blocking code.

However, if your actors have blocking operations like database read/write or file system read/write, 
we recommend that you use a dispatcher driven by the `tread-pool-executor`.  For example, the 
`PinnedDispatcher` is used in the Socko's file upload 
[example app](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/fileupload).

Isolating blocking operations to a thread pool means that other actors can continue processing without
waiting for your actor's blocking operation to finish.

The following code is taken from our file upload example application. Because `StaticFileProcessor`
and `FileUploadProcessor` reads and writes lots of files, we have set them up to use a `PinnedDispatcher`.
Note that we have only allocated 5 threads to each processor. To scale, you will need to allocate
more threads.

    val actorConfig = "my-pinned-dispatcher {\n" +
      "  type=PinnedDispatcher\n" +
      "  executor=thread-pool-executor\n" +
      "}\n" +
      "akka {\n" +
      "  event-handlers = [\"akka.event.slf4j.Slf4jEventHandler\"]\n" +
      "  loglevel=DEBUG\n" +
      "  actor {\n" +
      "    deployment {\n" +
      "      /static-file-router {\n" +
      "        router = round-robin\n" +
      "        nr-of-instances = 5\n" +
      "      }\n" +
      "      /file-upload-router {\n" +
      "        router = round-robin\n" +
      "        nr-of-instances = 5\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}"

    val actorSystem = ActorSystem("FileUploadExampleActorSystem", ConfigFactory.parseString(actorConfig))

    val staticFileProcessorRouter = actorSystem.actorOf(Props[StaticFileProcessor]
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")
    
    val fileUploadProcessorRouter = actorSystem.actorOf(Props[FileUploadProcessor]
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "file-upload-router")


