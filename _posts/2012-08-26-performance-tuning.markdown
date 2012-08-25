---
layout: post
title: Performance Tuning for Socko
summary: Make your Socko server go a little faster
author: Vibul
---

Was googling around when I came across this [advice](http://stackoverflow.com/questions/6856116/why-poor-performance-of-netty) for Netty performance tuning (Socko is build on top of Netty).

Trying using the Java [options](http://gleamynode.net/articles/2232/):

    java -server -Xms2048m -Xmx2048m -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods


Also, to support a large number of sockets, increase your [file descriptors](http://www.cs.uwaterloo.ca/~brecht/servers/openfiles.html).


