---
layout: post
title: Initial Benchmarks for Socko v0.1.0
summary: Socko vs Apache vs Tomcat
author: Vibul
---

## Setup
 - These tests were performed on my VMWare workstation 8 guest VM running:
   - Ubuntu 11.10 64 bit
   - Intel Core i7 CPU @ 2.67GHz x 2
   - 2.9 GiB RAM

 - VM was rebooted prior to the first test for each web server.

 - Each test was run 3 times and the best result displayed.

 - Web servers tested
   - Apache/2.2.20 (Ubuntu)
   - Apache tomcat 7.0.27 with default settings
   - Socko v0.1 [Benchmark example application](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/benchmark)
     run using `sbt`.
   
 - ApacheBench, Version 2.3 <$Revision: 655654 $> was used to perform the test
 
 - Java 1.6.0_26 64 bit
 
 
## Test 1. 1MB static file, 100 concurrent users, 10,000 requests

*Benchmark*

`ab -n10000 -c100 <url>`

*Results*
<table class="code">
  <tr>
    <th>Web Server</th>
    <th>Time taken for tests</th>
    <th>Requests per second (mean)</th>
  </tr>
  <tr>
    <td>Apache</td>
    <td>8.301 seconds</td>
    <td>1204.72 [#/sec] </td>
  </tr>
  <tr>
    <td>Socko</td>
    <td>10.666 seconds</td>
    <td>937.55 [#/sec] </td>
  </tr>
  <tr>
    <td>Tomcat</td>
    <td>19.409 seconds</td>
    <td>515.22 [#/sec] </td>
  </tr>
</table>


## Test 2. Dynamic content, 100 concurrent users, 100,000 requests

*Benchmark*

`ab -n100000 -c100 <url>`

*Results*
<table class="code">
  <tr>
    <th>Web Server</th>
    <th>Time taken for tests</th>
    <th>Requests per second (mean)</th>
  </tr>
  <tr>
    <td>Socko</td>
    <td>16.649 seconds</td>
    <td>6006.39 [#/sec] </td>
  </tr>
  <tr>
    <td>Tomcat</td>
    <td>22.347 seconds</td>
    <td>4474.82 [#/sec] </td>
  </tr>
</table>

*Tomcat JSP used in the test*

    <%@page session="false" contentType="text/html; charset=UTF-8" %>
    <html>
    <body>
    Hello
    </body>
    </html>

## Conclusion

These are very early tests.

It seems Socko is a little faster than Tomcat for static and dynamic content.

However, Socko is a little slower than Apache for static content.

I have noticed that Tomcat caches small files so that is something we will do for Socko v0.2.
Once in memory caching has been implemented, we will re-test for smaller static files of size.

