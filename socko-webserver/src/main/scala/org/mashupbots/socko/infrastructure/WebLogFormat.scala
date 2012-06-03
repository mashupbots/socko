//
// Copyright 2012 Vibul Imtarnasan, David Bolton and Socko contributors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.mashupbots.socko.infrastructure

/**
 * Web Server Activity Log setting
 */
object WebLogFormat extends Enumeration {

  /**
   * Log in common format. See http://en.wikipedia.org/wiki/Common_Log_Format
   *
   * For example:
   * {{{
   * 216.67.1.91 - leon [01/Jul/2002:12:11:52 +0000] "GET /index.html HTTP/1.1" 200 431 "http://www.loganalyzer.net/"
   * }}}
   */
  val Common = Value

  /**
   * Log in combined format. See http://httpd.apache.org/docs/current/logs.html. This is just the same as the common
   * format with added user agent and referrer.
   * 
   * For example:
   * {{{
   * 216.67.1.91 - leon [01/Jul/2002:12:11:52 +0000] "GET /index.html HTTP/1.1" 200 431 "http://www.loganalyzer.net/" "Mozilla/4.05 [en] (WinNT; I)"
   * }}}
   */
  val Combined = Value
  
  /**
   * Log in extended format. See http://www.w3.org/TR/WD-logfile.html
   *
   * For example:
   * {{{
   * #Software: Socko
   * #Version: 1.0
   * #Date: 2002-05-02 17:42:15
   * #Fields: date time c-ip cs-username s-ip s-port cs-method cs-uri-stem cs-uri-query sc-status sc-bytes cs-bytes time-taken cs(User-Agent) cs(Referrer)
   * 2002-05-24 20:18:01 172.224.24.114 - 206.73.118.24 80 GET /Default.htm - 200 7930 248 31 Mozilla/4.0+(compatible;+MSIE+5.01;+Windows+2000+Server) http://64.224.24.114/
   * }}}
   */
  val Extended = Value
}