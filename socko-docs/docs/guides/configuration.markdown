---
layout: docs
title: Socko User Guide - Configuration

HttpRequestEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpRequestEvent">HttpRequestEvent</a></code>
HttpChunkEventClass: <code><a href="../api/#org.mashupbots.socko.events.HttpChunkEvent">HttpChunkEvent</a></code>
WebSocketFrameEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketFrameEvent">WebSocketFrameEvent</a></code>
WebSocketHandshakeEventClass: <code><a href="../api/#org.mashupbots.socko.events.WebSocketHandshakeEvent">WebSocketHandshakeEvent</a></code>
WebServerClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServer">WebServer</a></code>
WebServerConfigClass: <code><a href="../api/#org.mashupbots.socko.webserver.WebServerConfig">WebServerConfig</a></code>
WebLogEventClass: <code><a href="../api/#org.mashupbots.socko.infrastructure.WebLogEvent">WebLogEvent</a></code>
WebLogWriterClass: <code><a href="../api/#org.mashupbots.socko.infrastructure.WebLogWriter">WebLogWriter</a></code>
---
# Socko User Guide - Configuration

## Introduction

Socko's configuration is defined in {{ page.WebServerConfigClass }}.

Web server configuration is immutable. To change the configuration, a new {{ page.WebServerClass }} class
must be instanced with the new configuration and started.

Socko configuration settings can be grouped as:
 - [Network](#Network)
 - [Web Logs](#WebLogs)
 - [SPDY](#SPDY)



## Loading Configuration

Configuration can be changed in [code](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/config/CodedConfigApp.scala)
or in the project's [Akka configuration file](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/config/AkkaConfigApp.scala).

For example, to change the port from the default `8888` to `7777` in code:

{% highlight scala %}
    val webServer = new WebServer(WebServerConfig(port=7777), routes)
{% endhighlight %}

To change the port to `9999` in your Akka configuration file, first define an object to load the settings
from `application.conf`. Note the setting will be named `akka-config-example`.

{% highlight scala %}
    object MyWebServerConfig extends ExtensionId[WebServerConfig] with ExtensionIdProvider {
      override def lookup = MyWebServerConfig
      override def createExtension(system: ExtendedActorSystem) =
        new WebServerConfig(system.settings.config, "akka-config-example")
    }
{% endhighlight %}

Then, start the actor system and load the configuration from that system.

{% highlight scala %}
    val actorSystem = ActorSystem("AkkaConfigActorSystem")
    val myWebServerConfig = MyWebServerConfig(actorSystem)
{% endhighlight %}
    
Lastly, add the following your `application.conf`

    akka-config-example {
        port=9999
    }

A complete example `application.conf` can be found in {{ page.WebServerConfigClass }}.




## Network Settings <a class="blank" id="Network"></a>

Common settings are:

 - `serverName`

   Human friendly name of this server. Defaults to `WebServer`.
    
 - `hostname`
 
   Hostname or IP address to bind. `0.0.0.0` will bind to all addresses. You can also specify comma 
   separated hostnames/ip address like `localhost,192.168.1.1`. Defaults to `localhost`.
   
 - `port`
 
   IP port number to bind to. Defaults to `8888`.
   
 - `idleConnectionTimeout`
 
   If a connection to the web server is idle (no read or writes) for the specified period of time, it
   will be closed. Defaults to `0` which indicates no timeout - i.e. connections are not closed.

 - `webLog`
   
   Web server activity log.
   
 - `ssl`
 
   Optional SSL configuration. Default is `None`.
   
 - `http`
 
   Optional HTTP request settings.

 - `tcp`
 
   Optional TCP/IP settings.

Refer to the api documentation of {{ page.WebServerConfigClass }} for all settings.




## Web Log Settings <a class="blank" id="WebLogs"></a>

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
      
        # Optional path of actor to which web log events will be sent for writing. If not specified, the default
        # web log writer will be created
        custom-actor-path = 

        # Optional web log format: Common (Default), Combined or Extended 
        format = Common
      }
    }
    
You can also turn it on programmatically as illustrated in the [web log example app](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/weblog/WebLogApp.scala).

{% highlight scala %}
    // Turn on web logs
    // Web logs will be written to the logger. You can control output via logback.xml.
    val config = WebServerConfig(webLog = Some(WebLogConfig()))
    val webServer = new WebServer(config, routes, actorSystem)
{% endhighlight %}
    
When turned on, the default behaviour is to write web logs to your installed [Akka logger](http://doc.akka.io/docs/akka/2.0.1/scala/logging.html) 
using {{ page.WebLogWriterClass }}. The Akka logger asynchronously writes to the log so it will not slow down 
your application down.

To activate Akka logging, add the following to `application.conf`:

    akka {
      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      loglevel = "DEBUG"
    }

You can configure where web logs are written by configuring your installed logger. For example, if you are using 
[Logback](http://logback.qos.ch/), you can write to a daily rolling file by changing `logback.xml` to include:

{% highlight xml %}
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
  
      <logger name="org.mashupbots.socko.infrastructure.WebLogWriter" level="info" additivity="false">
        <appender-ref ref="WEBLOG" />
      </logger>

      <root level="info">
        <appender-ref ref="STDOUT" />
      </root>
    </configuration>
{% endhighlight %}

### Recording Web Logs Events

Web log events can be recorded via the processing context.

 - {{ page.HttpRequestEventClass }}
 
   Web logs events are automatically recorded for you when you call `response.write()`, `response.writeLastChunk()` 
   or `response.redirect()` methods.

 - {{ page.HttpChunkEventClass }}

   As per {{ page.HttpRequestEventClass }}.
   
 - {{ page.WebSocketFrameEventClass }}
 
   Web log events are **NOT** automatically recorded for you. This is because web sockets do not strictly follow 
   the request/response structure of HTTP. For example, in a chat server, a broadcast message will not have a request
   frame.
   
   If you wish to record a web log event, you can call `writeWebLog()`. The method, URI and other details
   of the event to be recorded is arbitrarily set by you.
   
 - {{ page.WebSocketHandshakeEventClass }}
   
   Web log events are automatically recorded for you.
   

### Custom Web Log Output

If you prefer to use your own method and/or format of writing web logs, you can specify the path of a custom actor 
to recieve {{ page.WebLogEventClass }} messages in your `application.conf`.

    akka-config-example {
      server-name=AkkaConfigExample
      hostname=localhost
      port=9000
      web-log {
        custom-actor-path = "akka://my-system/user/my-web-log-writer"
      }
    }

For more details, refer to the [custom web log example app](https://github.com/mashupbots/socko/blob/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/weblog/CustomWebLogApp.scala).




## SPDY Settings <a class="blank" id="SPDY"></a>

[SPDY](http://en.wikipedia.org/wiki/SPDY) is an experimental networking protocol used in speeding up delivery of web
content.

It is currently supported in the Chrome and Firefox (v11+) browsers.

Steps to enabling SPDY:

 1. You will need to run with **JDK 7**
 
 2. Setup JVM Bootup classes
    
    SPDY uses a special extension to TLS/SSL called [Next Protocol Negotiation](http://tools.ietf.org/html/draft-agl-tls-nextprotoneg-00).
    This is not currently supported by Java JDK. However, the Jetty team has kindly open sourced their implementation. 
    
    Refer to [Jetty NPN](http://wiki.eclipse.org/Jetty/Feature/NPN#Versions) for the correct version and download it from
    the [maven repository](http://repo2.maven.org/maven2/org/mortbay/jetty/npn/npn-boot/).
    
    Add the JAR to your JVM boot parameters: `-Xbootclasspath/p:/path/to/npn-boot-1.0.0.v20120402.jar`.
 
 3. Set Web Server Configuration
 
    You will need to turn on SSL and enable SPDY in your configuration.

    {% highlight scala %}
        val keyStoreFile = new File(contentDir, "testKeyStore")
        val keyStoreFilePassword = "password"
        val sslConfig = SslConfig(keyStoreFile, keyStoreFilePassword, None, None)
        val httpConfig = HttpConfig(spdyEnabled = true)
        val webServerConfig = WebServerConfig(hostname="0.0.0.0", webLog = Some(WebLogConfig()), ssl = Some(sslConfig), http = httpConfig)
        val webServer = new WebServer(webServerConfig, routes, actorSystem)
        webServer.start()
    {% endhighlight %}
    



