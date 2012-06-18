---
layout: post
title: How we got SPDY working with Netty 3.5 for Socko
summary: Post intended for Netty coders who wish to add SPDY to their apps
author: Vibul
---

This post intended for **Netty coders** who wish to add SPDY to their apps.

If you wish to know how to use SPDY in **Socko**, refer to the [Socko User Guide](/docs/0.2.0/guides/user_guide.html#SPDY).

Jos Dirksen did most of the hard work for us in his [post](http://www.smartjava.org/content/using-spdy-and-http-transparently-using-netty).
We found that we needed to modify a few of Jos's steps for Netty 3.5.

Also, most of the code examples are is in Scala because that is what we are using to write Socko.

## Step 1 - Add Support for Next Protocol Negotiation (NPN)
 
When you type `https://www.mywebsite.com/mypage` into the Chrome browser address bar, Chrome asks the server if it supports 
SPDY and if so, which version.

To achieve this, a special TLS/SSL extension called [Next Protocol Negotiation](http://tools.ietf.org/html/draft-agl-tls-nextprotoneg-00)
is used.

NPN is not currently supported by Java JDK. However, the Jetty team has kindly open sourced their 
implementation. Refer to [Jetty NPN](http://wiki.eclipse.org/Jetty/Feature/NPN#Versions) for the correct version and 
download the JAR from the [maven repository](http://repo2.maven.org/maven2/org/mortbay/jetty/npn/npn-boot/).

Next, download and install **JDK 7**.

Finally, add the JAR to your JVM start up parameters: 

    java -Xbootclasspath/p:/path/to/npn-boot-1.0.0.v20120402.jar ....


## Step 2 - Hooking up NPN to Netty

To use NPN from our app, we first have to add the Jetty NPN-API JAR.

{% highlight xml %}
    <dependency>
	  <groupId>org.eclipse.jetty.npn</groupId>
	  <artifactId>npn-api</artifactId>
	  <version>1.0.0.v20120402</version>
    </dependency> 
{% endhighlight %}

For Socko, we just added the source file `NextProtoNego.java` into our project to save our users having to add another
dependency. 

Next, we need use the NPN API to specify our supported protocols and receive notification of the selected protocol. This is 
through implementing the NPN `ServerProvider` interface.

{% highlight java %}
    public class SpdyServerProvider implements ServerProvider {
        private String selectedProtocol = null;
        
        public void unsupported() {
            // if unsupported, default to http/1.1
            selectedProtocol = "http/1.1";
        }

        public List<String> protocols() {
            return Arrays.asList("spdy/3", "spdy/2", "http/1.1");
        }

        public void protocolSelected(String protocol) {
            selectedProtocol = protocol;
        }

        public String getSelectedProtocol() {
            return selectedProtocol;
        }
    }
{% endhighlight %}
    
As you can see, we will support SPDY version 3, SPDY version 2 and HTTP 1.1.


## Step 3 - Setup Netty Pipeline

Setting up a SPDY pipeline involves:

 1. Setup TLS/SSL in the normal way
 2. Specifying our `SpdyServerProvider` to NPN.
 3. We also turn on NPN debug if the log level for this class is set to DEBUG.
 4. Adding a special `ProtocolNegoitationHandler` to process the results of protocol negotiation

In Socko, we setup a standard HTTP pipeline unless SPDY is enabled.

{% highlight scala %}
    class PipelineFactory(server: WebServer) extends ChannelPipelineFactory with Logger {
      def getPipeline(): ChannelPipeline = {
        if (server.config.http.spdyEnabled) getSpdyPipeline() else getHttpPipeline()
      }

      private def getHttpPipeline(): ChannelPipeline = {
        ...
      }

      private def getSpdyPipeline(): ChannelPipeline = {
        val newPipeline = Channels.pipeline()

        val sslEngine = server.sslManager.get.createSSLEngine()
        newPipeline.addLast("ssl", new SslHandler(sslEngine))

        NextProtoNego.put(sslEngine, new SpdyServerProvider())
        NextProtoNego.debug = log.isDebugEnabled

        newPipeline.addLast("pipeLineSelector", new ProtocolNegoitationHandler(server))

        return newPipeline
      }
    }
{% endhighlight %}


## Step 4 - Implementing ProtocolNegoitationHandler

The protocol negotiation handler receives numerous events during the protocol negotiation process.

{% highlight scala %}
    class ProtocolNegoitationHandler(server: WebServer) extends ChannelUpstreamHandler with Logger {

      def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent) {

        val pipeline: ChannelPipeline = ctx.getPipeline()
        val handler = pipeline.get(classOf[SslHandler])
        val provider = NextProtoNego.get(handler.getEngine).asInstanceOf[SpdyServerProvider]
        val selectedProtocol = provider.getSelectedProtocol
        val httpConfig = server.config.http

        // Null is returned during the negotiation process so ignore it
        if (selectedProtocol == null) {
          // If channel open event (see SimpleChannelUpstreamHandler for sample), add to all channels
          // For some reason, there are several channels opened for SPDY for the browser.
          if (e.isInstanceOf[ChannelStateEvent]) {
            val evt = e.asInstanceOf[ChannelStateEvent]
            if (evt.getState() == ChannelState.OPEN) {
              server.allChannels.add(e.getChannel)
            }
          }
        } else {
          if (selectedProtocol.startsWith("spdy/")) {
            val version = Integer.parseInt(selectedProtocol.substring(5))

            pipeline.addLast("decoder", new SpdyFrameDecoder(version, httpConfig.maxChunkSizeInBytes,
              httpConfig.maxHeaderSizeInBytes))
            pipeline.addLast("spdy_encoder", new SpdyFrameEncoder(version))
            pipeline.addLast("spdy_session_handler", new SpdySessionHandler(version, true))
            pipeline.addLast("spdy_http_encoder", new SpdyHttpEncoder(version))
            pipeline.addLast("spdy_http_decoder", new SpdyHttpDecoder(version, httpConfig.maxLengthInBytes))
            pipeline.addLast("chunkWriter", new ChunkedWriteHandler())
            pipeline.addLast("handler", new RequestHandler(server))

            // remove this handler, and process the requests as SPDY
            pipeline.remove(this)
            ctx.sendUpstream(e)
          } else if (selectedProtocol == "http/1.1") {
            pipeline.addLast("decoder", new HttpRequestDecoder(httpConfig.maxInitialLineLength,
              httpConfig.maxHeaderSizeInBytes, httpConfig.maxChunkSizeInBytes))
            if (httpConfig.aggreateChunks) {
              pipeline.addLast("chunkAggregator", new HttpChunkAggregator(httpConfig.maxLengthInBytes))
            }
            pipeline.addLast("encoder", new HttpResponseEncoder())
            pipeline.addLast("chunkWriter", new ChunkedWriteHandler())
            pipeline.addLast("handler", new RequestHandler(server))

            // remove this handler, and process the requests as HTTP
            pipeline.remove(this);
            ctx.sendUpstream(e);
          } else {
            throw new UnsupportedOperationException("Unsupported protocol: " + selectedProtocol)
          }
        }
      }
    }
{% endhighlight %}


### Checking for the Selected Protocol

To check if a protocol has been selected, we check the `selectedProtocol` property of the `SpdyServerProvider` object
associated with the `SSLEngine` for this instance of the pipeline.

If it is `null`, a protocol is yet to be selected.

### Storing Opened Channels

We also check for the open channel event so that we can add the channel to a Netty channel group. This is important 
because we found that several channels are created during NPN negotiation process by a single Chrome tab - even if
a single resource was requested.

Adding the channel to our `server.allChannels` group means that upon web server exit, we can close these channels. If 
you do not close these channels, Netty will hang on `channelFactory.releaseExternalResources()` when shutting down.

### Post Negotiation

After a protocol has been selected, we can remove `ProtocolNegoitationHandler` from the pipeline and replace it
with the SPDY pipeline (thanks to Twitter and Jeff Pinner for donating it to Netty!).

The SPDY pipeline encodes/decodes between **SPDY frames** and Netty's **HttpRequest/HttpResponse/HttpChunk** messages. In this
way, you can use your normal HTTP handlers.



## Step 5 - Writing HTTP Responses

### SPDY Headers

You will need to copy the following SPDY headers from the request to the response.

{% highlight scala %}
    var spdyId = event.request.headers.getOrElse(SpdyHttpHeaders.Names.STREAM_ID, "")
    response.setHeader(SpdyHttpHeaders.Names.STREAM_ID, spdyId)
    response.setHeader(SpdyHttpHeaders.Names.PRIORITY, 0);
{% endhighlight %}

### Response Content

You will need to set the content of the response within `DefaultHttpResponse`.

{% highlight scala %}
    val response = new DefaultHttpResponse(HttpVersion.valueOf(event.request.httpVersion), HttpResponseStatus.OK.toNetty)
    response.setContent(ChannelBuffers.wrappedBuffer(content))
{% endhighlight %}

Writing the body to the channel after writing the response is **not supported**. I guess this is because the SPDY 
encoder does not have the context to correctly encode arbitrary binary data.

{% highlight scala %}
    // Not Supported
    val response = new DefaultHttpResponse(HttpVersion.valueOf(event.request.httpVersion), HttpResponseStatus.OK.toNetty)
    ch.write(response)
    ch.write(ChannelBuffers.wrappedBuffer(content))
{% endhighlight %}

### Chunk Response Content

For large files and/or streaming content, setting the response content is not feasible.  You don't want all that data
in memory.

We found using `HttpChunks` to be the best solution.

First we need to modify Netty's `ChunkedFile` so that instead of writing binary chunks, it writes a `HttpChunk` message 
containing the chunked content.  One small complication is that we need to keep track of and send the trailer chunk to 
notify the client of the end of stream.

{% highlight java %}
    public class HttpChunkedFile implements ChunkedInput {

        private final RandomAccessFile file;
        private final long startOffset;
        private final long endOffset;
        private final int chunkSize;
        private volatile long offset;
        private volatile boolean sentLastChunk = false;
        static final int DEFAULT_CHUNK_SIZE = 8192;

        /**
         * Creates a new instance that fetches data from the specified file.
         */
        public HttpChunkedFile(File file) throws IOException {
            this(file, DEFAULT_CHUNK_SIZE);
        }

        /**
         * Creates a new instance that fetches data from the specified file.
         *
         * @param chunkSize the number of bytes to fetch on each
         *                  {@link #nextChunk()} call
         */
        public HttpChunkedFile(File file, int chunkSize) throws IOException {
            this(new RandomAccessFile(file, "r"), chunkSize);
        }

        /**
         * Creates a new instance that fetches data from the specified file.
         */
        public HttpChunkedFile(RandomAccessFile file) throws IOException {
            this(file, DEFAULT_CHUNK_SIZE);
        }

        /**
         * Creates a new instance that fetches data from the specified file.
         *
         * @param chunkSize the number of bytes to fetch on each
         *                  {@link #nextChunk()} call
         */
        public HttpChunkedFile(RandomAccessFile file, int chunkSize) throws IOException {
            this(file, 0, file.length(), chunkSize);
        }

        /**
         * Creates a new instance that fetches data from the specified file.
         *
         * @param offset the offset of the file where the transfer begins
         * @param length the number of bytes to transfer
         * @param chunkSize the number of bytes to fetch on each
         *                  {@link #nextChunk()} call
         */
        public HttpChunkedFile(RandomAccessFile file, long offset, long length, int chunkSize) throws IOException {
            if (file == null) {
                throw new NullPointerException("file");
            }
            if (offset < 0) {
                throw new IllegalArgumentException(
                        "offset: " + offset + " (expected: 0 or greater)");
            }
            if (length < 0) {
                throw new IllegalArgumentException(
                        "length: " + length + " (expected: 0 or greater)");
            }
            if (chunkSize <= 0) {
                throw new IllegalArgumentException(
                        "chunkSize: " + chunkSize +
                        " (expected: a positive integer)");
            }

            this.file = file;
            this.offset = startOffset = offset;
            endOffset = offset + length;
            this.chunkSize = chunkSize;

            file.seek(offset);
        }

        /**
         * Returns the offset in the file where the transfer began.
         */
        public long getStartOffset() {
            return startOffset;
        }

        /**
         * Returns the offset in the file where the transfer will end.
         */
        public long getEndOffset() {
            return endOffset;
        }

        /**
         * Returns the offset in the file where the transfer is happening currently.
         */
        public long getCurrentOffset() {
            return offset;
        }

        public boolean hasNextChunk() throws Exception {
            if (offset < endOffset && file.getChannel().isOpen()) {
            	return true;
            } else {
            	return !sentLastChunk;
            }
        }

        public boolean isEndOfInput() throws Exception {
            return !hasNextChunk();
        }

        public void close() throws Exception {
            file.close();
        }

        public Object nextChunk() throws Exception {
            long offset = this.offset;
            if (offset >= endOffset) {
            	if (sentLastChunk){
                    return null;        		
            	} else {
            		sentLastChunk = true;
            		return new DefaultHttpChunkTrailer();
            	}
            }

            int chunkSize = (int) Math.min(this.chunkSize, endOffset - offset);
            byte[] chunk = new byte[chunkSize];
            file.readFully(chunk);
            this.offset = offset + chunkSize;
            return new DefaultHttpChunk(wrappedBuffer(chunk));
        }
    }
{% endhighlight %}

Then, make sure you add Netty's `ChunkedWriteHandler` to your pipeline:

{% highlight scala %}
    pipeline.addLast("chunkWriter", new ChunkedWriteHandler())
{% endhighlight %}

Finally, write the chunked response as follows:

{% highlight scala %}
    val response = new DefaultHttpResponse(HttpVersion.valueOf(event.request.httpVersion), HttpResponseStatus.OK.toNetty)
    response.setChunked(true);
    response.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
    ch.write(response)
    
    val writeFuture = ch.write(new HttpChunkedFile(raf, 0, fileLength, 8192))
    if (!event.request.isKeepAlive) {
      writeFuture.addListener(ChannelFutureListener.CLOSE)
    }
{% endhighlight %}

If you wish to write a stream, you need only to convert Netty's `ChunkedStream` to `HttpChunkStream` in the same way
we converted `ChunkedFile`.





