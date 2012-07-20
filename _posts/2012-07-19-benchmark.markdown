---
layout: post
title: Benchmarks for Socko v0.2.0
summary: Socko vs Apache vs Tomcat vs VertX
author: Vibul
---

## Disclaimer
My benchmarking is not very rigorous
 - All tests were performed on a single VM on my desktop. Better benchmarking would use several servers.
 - Only static files download were tested. Better benchmarking should include other scenarios.
 - I'm not an expert in other web servers and have used their default configuration without optimization 

## Setup
 - These tests were performed on my VMWare workstation 8 8.0.4 guest VM running:
   - Ubuntu 12.04 64 bit
   - Intel Core i7 CPU 920 @ 2.67GHz × 8 (i.e. 8 cores)
   - 2.9 GiB RAM

 - VM was rebooted prior to the first test for each web server.

 - Each test was run 3 times and the best result displayed.

 - Web servers tested
   - Apache/2.2.20 (Ubuntu)
   - Apache tomcat 7.0.27 with default settings
   - Socko v0.2 [Benchmark example application](https://github.com/mashupbots/socko/tree/master/socko-examples/src/main/scala/org/mashupbots/socko/examples/benchmark)
     run using `sbt`.
   - VertX 1.0.1. The `httpperf` example was run with 6 instances as per its instructions. `foo.html` was changed to the
     relevant file size for each run.
   
 - ApacheBench, Version 2.3 <$Revision: 655654 $> was used to perform the test
 
 - Java version "1.7.0_03"
   - OpenJDK Runtime Environment (IcedTea7 2.1.1pre) (7~u3-2.1.1~pre1-1ubuntu3)
   - OpenJDK 64-Bit Server VM (build 22.0-b10, mixed mode)
 
 
## Test #1 - 87 bytes static file, 100 concurrent users, 500,000 requests

*Benchmark*

`ab -n500000 -c100 <url>`

*Results*
<table class="code">
  <tr>
    <th>Web Server</th>
    <th>Time taken for tests</th>
    <th>Requests per second (mean)</th>
  </tr>
  <tr>
    <td>VertX</td>
    <td>41.391 seconds</td>
    <td>12079.99 [#/sec] </td>
  </tr>
  <tr>
    <td>Tomcat</td>
    <td>46.029 seconds</td>
    <td>10862.63 [#/sec] </td>
  </tr>
  <tr>
    <td>Socko</td>
    <td>46.139 seconds</td>
    <td>10836.92 [#/sec] </td>
  </tr>
  <tr>
    <td>Apache</td>
    <td>48.777 seconds</td>
    <td>10250.74 [#/sec] </td>
  </tr>
</table>


## Test #2 - 200K static file, 100 concurrent users, 100,000 requests

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
    <td>18.325 seconds</td>
    <td>5457.01 [#/sec] </td>
  </tr>
  <tr>
    <td>Apache</td>
    <td>18.444 seconds</td>
    <td>5421.95 [#/sec] </td>
  </tr>
  <tr>
    <td>VertX</td>
    <td>19.734 seconds</td>
    <td>5067.43 [#/sec] </td>
  </tr>
  <tr>
    <td>Tomcat</td>
    <td>22.724 seconds</td>
    <td>4400.65 [#/sec] </td>
  </tr>
</table>


## Test #3 - 1MB static file, 100 concurrent users, 20,000 requests

*Benchmark*

`ab -n20000 -c100 <url>`

*Results*
<table class="code">
  <tr>
    <th>Web Server</th>
    <th>Time taken for tests</th>
    <th>Requests per second (mean)</th>
  </tr>
  <tr>
    <td>Socko</td>
    <td>16.608 seconds</td>
    <td>1204.25 [#/sec] </td>
  </tr>
  <tr>
    <td>Apache</td>
    <td>18.757 seconds</td>
    <td>1066.26 [#/sec] </td>
  </tr>
  <tr>
    <td>Tomcat</td>
    <td>26.123 seconds</td>
    <td>765.62 [#/sec] </td>
  </tr>
  <tr>
    <td>VertX</td>
    <td>35.905 seconds</td>
    <td>557.03 [#/sec] </td>
  </tr>
</table>



## Conclusions

With further optimizations of other web servers, I'm sure they will give better results.

Hence, I'm **NOT** going to claim that Socko is the fastest server ever. 

However, I think we can conclude that Socko is doing OK on the HTTP file download front. Thanks to Netty, 
I think the throughput is acceptable.




