//
// Copyright 2012 Vibul Imtarnasan, David Bolton and Socko contributors.
//
// Licensed under the Apache License, Version 2.0 (the "License")
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
package org.mashupbots.socko.context

/**
 * Port of Netty's HttpResponseStatus class for convenience. Socko user won't have to refer to Netty javadoc.
 *
 * @param code HTTP response status code as per [[http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html RFC 2616]].
 */
case class HttpResponseStatus(code: Int) {

  private val nettyHttpResponseStatus = org.jboss.netty.handler.codec.http.HttpResponseStatus.valueOf(code)

  val reasonPhrase = nettyHttpResponseStatus.getReasonPhrase

  def toNetty():org.jboss.netty.handler.codec.http.HttpResponseStatus = {
    nettyHttpResponseStatus
  }
  
  override def toString(): String = {
    nettyHttpResponseStatus.toString
  }
}

object HttpResponseStatus {
  /**
   * 100 Continue
   */
  val CONTINUE = new HttpResponseStatus(100)

  /**
   * 101 Switching Protocols
   */
  val SWITCHING_PROTOCOLS = new HttpResponseStatus(101)

  /**
   * 102 Processing (WebDAV, RFC2518)
   */
  val PROCESSING = new HttpResponseStatus(102)

  /**
   * 200 OK
   */
  val OK = new HttpResponseStatus(200)

  /**
   * 201 Created
   */
  val CREATED = new HttpResponseStatus(201)

  /**
   * 202 Accepted
   */
  val ACCEPTED = new HttpResponseStatus(202)

  /**
   * 203 Non-Authoritative Information (since HTTP/1.1)
   */
  val NON_AUTHORITATIVE_INFORMATION = new HttpResponseStatus(203)

  /**
   * 204 No Content
   */
  val NO_CONTENT = new HttpResponseStatus(204)

  /**
   * 205 Reset Content
   */
  val RESET_CONTENT = new HttpResponseStatus(205)

  /**
   * 206 Partial Content
   */
  val PARTIAL_CONTENT = new HttpResponseStatus(206)

  /**
   * 207 Multi-Status (WebDAV, RFC2518)
   */
  val MULTI_STATUS = new HttpResponseStatus(207)

  /**
   * 300 Multiple Choices
   */
  val MULTIPLE_CHOICES = new HttpResponseStatus(300)

  /**
   * 301 Moved Permanently
   */
  val MOVED_PERMANENTLY = new HttpResponseStatus(301)

  /**
   * 302 Found
   */
  val FOUND = new HttpResponseStatus(302)

  /**
   * 303 See Other (since HTTP/1.1)
   */
  val SEE_OTHER = new HttpResponseStatus(303)

  /**
   * 304 Not Modified
   */
  val NOT_MODIFIED = new HttpResponseStatus(304)

  /**
   * 305 Use Proxy (since HTTP/1.1)
   */
  val USE_PROXY = new HttpResponseStatus(305)

  /**
   * 307 Temporary Redirect (since HTTP/1.1)
   */
  val TEMPORARY_REDIRECT = new HttpResponseStatus(307)

  /**
   * 400 Bad Request
   */
  val BAD_REQUEST = new HttpResponseStatus(400)

  /**
   * 401 Unauthorized
   */
  val UNAUTHORIZED = new HttpResponseStatus(401)

  /**
   * 402 Payment Required
   */
  val PAYMENT_REQUIRED = new HttpResponseStatus(402)

  /**
   * 403 Forbidden
   */
  val FORBIDDEN = new HttpResponseStatus(403)

  /**
   * 404 Not Found
   */
  val NOT_FOUND = new HttpResponseStatus(404)

  /**
   * 405 Method Not Allowed
   */
  val METHOD_NOT_ALLOWED = new HttpResponseStatus(405)

  /**
   * 406 Not Acceptable
   */
  val NOT_ACCEPTABLE = new HttpResponseStatus(406)

  /**
   * 407 Proxy Authentication Required
   */
  val PROXY_AUTHENTICATION_REQUIRED = new HttpResponseStatus(407)

  /**
   * 408 Request Timeout
   */
  val REQUEST_TIMEOUT = new HttpResponseStatus(408)

  /**
   * 409 Conflict
   */
  val CONFLICT = new HttpResponseStatus(409)

  /**
   * 410 Gone
   */
  val GONE = new HttpResponseStatus(410)

  /**
   * 411 Length Required
   */
  val LENGTH_REQUIRED = new HttpResponseStatus(411)

  /**
   * 412 Precondition Failed
   */
  val PRECONDITION_FAILED = new HttpResponseStatus(412)

  /**
   * 413 Request Entity Too Large
   */
  val REQUEST_ENTITY_TOO_LARGE = new HttpResponseStatus(413)

  /**
   * 414 Request-URI Too Long
   */
  val REQUEST_URI_TOO_LONG = new HttpResponseStatus(414)

  /**
   * 415 Unsupported Media Type
   */
  val UNSUPPORTED_MEDIA_TYPE = new HttpResponseStatus(415)

  /**
   * 416 Requested Range Not Satisfiable
   */
  val REQUESTED_RANGE_NOT_SATISFIABLE = new HttpResponseStatus(416)

  /**
   * 417 Expectation Failed
   */
  val EXPECTATION_FAILED = new HttpResponseStatus(417)

  /**
   * 422 Unprocessable Entity (WebDAV, RFC4918)
   */
  val UNPROCESSABLE_ENTITY = new HttpResponseStatus(422)

  /**
   * 423 Locked (WebDAV, RFC4918)
   */
  val LOCKED = new HttpResponseStatus(423)

  /**
   * 424 Failed Dependency (WebDAV, RFC4918)
   */
  val FAILED_DEPENDENCY = new HttpResponseStatus(424)

  /**
   * 425 Unordered Collection (WebDAV, RFC3648)
   */
  val UNORDERED_COLLECTION = new HttpResponseStatus(425)

  /**
   * 426 Upgrade Required (RFC2817)
   */
  val UPGRADE_REQUIRED = new HttpResponseStatus(426)

  /**
   * 500 Internal Server Error
   */
  val INTERNAL_SERVER_ERROR = new HttpResponseStatus(500)

  /**
   * 501 Not Implemented
   */
  val NOT_IMPLEMENTED = new HttpResponseStatus(501)

  /**
   * 502 Bad Gateway
   */
  val BAD_GATEWAY = new HttpResponseStatus(502)

  /**
   * 503 Service Unavailable
   */
  val SERVICE_UNAVAILABLE = new HttpResponseStatus(503)

  /**
   * 504 Gateway Timeout
   */
  val GATEWAY_TIMEOUT = new HttpResponseStatus(504)

  /**
   * 505 HTTP Version Not Supported
   */
  val HTTP_VERSION_NOT_SUPPORTED = new HttpResponseStatus(505)

  /**
   * 506 Variant Also Negotiates (RFC2295)
   */
  val VARIANT_ALSO_NEGOTIATES = new HttpResponseStatus(506)

  /**
   * 507 Insufficient Storage (WebDAV, RFC4918)
   */
  val INSUFFICIENT_STORAGE = new HttpResponseStatus(507)

  /**
   * 510 Not Extended (RFC2774)
   */
  val NOT_EXTENDED = new HttpResponseStatus(510)

}