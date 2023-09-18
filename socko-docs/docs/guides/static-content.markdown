---
layout: docs
title: Socko User Guide - Serving Static HTTP Content

StaticContentHandlerClass: <code><a href="../api/#org.mashupbots.socko.handlers.StaticContentHandler">StaticContentHandler</a></code>
StaticContentHandlerConfigClass: <code><a href="../api/#org.mashupbots.socko.handlers.StaticContentHandlerConfig">StaticContentHandlerConfig</a></code>
StaticFileRequestClass: <code><a href="../api/#org.mashupbots.socko.handlers.StaticFileRequest">StaticFileRequest</a></code>
StaticResourceRequestClass: <code><a href="../api/#org.mashupbots.socko.handlers.StaticResourceRequest">StaticResourceRequest</a></code>
---
# Socko User Guide - Serving Static HTTP Content

## Table of Contents

Socko's {{ page.StaticContentHandlerClass }} is used for serving static files and resources.

It supports HTTP compression, browser cache control and content caching.

 - [Actor Setup](#ActorSetup)
 - [Configuration](#Configuration)
 - [Handling Requests](#Requests)

## Actor Setup <a class="blank" id="ActorSetup"></a>

We recommend that you run {{ page.StaticContentHandlerClass }} with a router and with its own dispatcher.  This is
because {{ page.StaticContentHandlerClass }} contains blocking IO that must be isolated from other non-blocking 
actors.

For example, you can use the [Pinned Dispatcher](http://doc.akka.io/api/akka/2.1.2/index.html#akka.dispatch.PinnedDispatcher)
that dedicates a unique thread for each actor passed in as reference:

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
              router = round-robin-pool
              nr-of-instances = 5
            }
          }
        }
      }"""

    val actorSystem = ActorSystem("FileUploadExampleActorSystem", ConfigFactory.parseString(actorConfig))

    val handlerConfig = StaticContentHandlerConfig(
      rootFilePaths = Seq(contentDir.getAbsolutePath),
      tempDir = tempDir)

    val staticContentHandlerRouter = actorSystem.actorOf(Props(new StaticContentHandler(handlerConfig))
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")
{% endhighlight %}




## Configuration <a class="blank" id="Configuration"></a>

The {{ page.StaticContentHandlerConfigClass }} class is used to configure the serving of your static content.

Common settings are:

 - `rootFilePaths`

   List of root paths from while static files can be served. This is enforced to stop relative path type attacks; 
   e.g. `../etc/passwd`.  It does not apply to serving static resources.
    
 - `tempDir`
 
   Temporary directory where compressed files can be stored. Defaults to the `java.io.tmpdir` system property value.

Like other Socko configurations, you can optionally load these settings from  your Akka configuration file.

For example:

{% highlight scala %}
    val actorConfig = """
      my-pinned-dispatcher {
        type=PinnedDispatcher
        executor=thread-pool-executor
      }
      my-static-content-handler {
		    root-file-paths="/tmp/x1, /tmp/x2"
		  }
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel=DEBUG
        actor {
          deployment {
            /static-file-router {
              router = round-robin-pool
              nr-of-instances = 5
            }
          }
        }
      }"""

    val actorSystem = ActorSystem("FileUploadExampleActorSystem", ConfigFactory.parseString(actorConfig))

    val handlerConfig = MyStaticHandlerConfig(actorSystem)

    val staticContentHandlerRouter = actorSystem.actorOf(Props(new StaticContentHandler(handlerConfig))
      .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")

    ...

    object MyStaticHandlerConfig extends ExtensionId[StaticContentHandlerConfig] with ExtensionIdProvider {
      override def lookup = MyStaticHandlerConfig
      override def createExtension(system: ExtendedActorSystem) =
        new StaticContentHandlerConfig(system.settings.config, "my-static-content-handler")
    }

{% endhighlight %}

Refer to the [scala doc](../api/#org.mashupbots.socko.handlers.StaticContentHandlerConfig) for more information.




## Handling Requests <a class="blank" id="Requests"></a>

To serve a file or resource, send {{ page.StaticFileRequestClass }} or {{ page.StaticResourceRequestClass }} to
the router.

{% highlight scala %}
    val routes = Routes({
      case HttpRequest(request) => request match {
        case GET(Path("/foo.html")) => {
          staticContentHandlerRouter ! new StaticFileRequest(request, new File("/my/path/", "foo.html"))
        }
        case GET(Path("/foo.txt")) => {
          staticContentHandlerRouter ! new StaticResourceRequest(request, "META-INF/foo.txt")
        }
      }
    })
{% endhighlight %}

Note that for {{ page.StaticResourceRequestClass }}, do not put the leading slash `/` in the path.
(This is because behind the scenes we are using [ClassLoader.getResourceAsStream()](http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html?page=2))


