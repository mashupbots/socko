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
package org.mashupbots.socko.routes

import scala.util.matching.Regex

import org.mashupbots.socko.events.HttpChunkEvent
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.SockoEvent
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.mashupbots.socko.events.WebSocketHandshakeEvent

/**
 * Routes define the rules for dispatching events to its intended Akka handlers. It is implemented as a
 * list of PartialFunctions.
 *
 * To assist with routing, Socko has the following extractors:
 *  - HTTP Method: GET, POST, PUT, DELETE, HEAD, CONNECT, OPTIONS, TRACE
 *  - HTTP Path: Path, PathSegments, PathRegex
 *  - HTTP Host: Host, HostSegments, HostRegex
 *  - HTTP Query String: QueryString, QueryStringRegex
 *
 * Example of a single list of partial functions:
 * {{{
 *   val r = Routes({
 *     case GET(PathSegments("record" :: id :: Nil)) => {
 *       ...
 *     }
 *     case PathSegments("record" :: id :: Nil) => {
 *       ...
 *     }
 *   })
 * }}}
 *
 * Example of 2 lists of partial functions:
 * {{{
 *   val r = Routes(
 *     {
 *       case GET(PathSegments("record" :: id :: Nil)) => {
 *         ...
 *       }
 *       case PathSegments("record" :: id :: Nil) => {
 *         ...
 *       }
 *     },
 *     {
 *       case PUT(Host("aaa.abc.com")) & Path("/test1") => {
 *         ...
 *       }
 *       case Host("aaa.abc.com") & Path("/test2") => {
 *         ...
 *       }
 *     })
 * }}}
 *
 * This area of code uses extractors, also known as the `unapply` method. It is important to understand
 * how to use these patterns.
 *  - `event @` is scala's variable binding pattern. It will be assigned the value of the matching SockoEvent
 *  - `GET(x)` and `Path(y)` are extractors. In the case of `GET, `x` is the same SockoEvent as `event`. For `Path`,
 *     `y` is the path as a string. In other words, the value in between the parentheses is the return value of the
 *     associated `unapply` method.
 *  - `&` chains together extractors like a logical `AND`
 *
 * This [[http://daily-scala.blogspot.com.au/2010/02/chaining-partial-functions-with-orelse.html article]]
 * explains how chaining of `PartialFunction` works using `orElse`.
 *
 */
object Routes {
  def apply(funcList: PartialFunction[SockoEvent, Unit]*) = {
    funcList.toList.reduceLeft { (functions, f) => functions orElse f }
  }
}

/**
 * Used to help match an HTTP Request event.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case HttpRequest(httpRequest) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object HttpRequest {
  def unapply(ctx: SockoEvent) =
    if (ctx.isInstanceOf[HttpRequestEvent]) Some(ctx.asInstanceOf[HttpRequestEvent])
    else None
}

/**
 * Used to help match an WebSocket Handshake event.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case WebSocketHandshake(wsHandshake) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object WebSocketHandshake {
  def unapply(ctx: SockoEvent) =
    if (ctx.isInstanceOf[WebSocketHandshakeEvent]) Some(ctx.asInstanceOf[WebSocketHandshakeEvent])
    else None
}

/**
 * Used to help match an WebSocket Frame event.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case WebSocketFrame(wsFrame) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object WebSocketFrame {
  def unapply(ctx: SockoEvent) =
    if (ctx.isInstanceOf[WebSocketFrameEvent]) Some(ctx.asInstanceOf[WebSocketFrameEvent])
    else None
}

/**
 * Used to help match an HTTP Chunk event.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case HttpChunk(httpChunk) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object HttpChunk {
  def unapply(ctx: SockoEvent) =
    if (ctx.isInstanceOf[HttpChunkEvent]) Some(ctx.asInstanceOf[HttpChunkEvent])
    else None
}

/**
 * Used to help match the "method" of the HTTP end point 
 *
 * You should not need to use this class. Rather use the objects that extends from this class. For example:
 * [[org.mashupbots.socko.routes.GET]].
 *
 * @param method Name of the HTTP method
 */
class Method(method: String) {
  def unapply(ctx: SockoEvent) =
    if (ctx.endPoint.method.equalsIgnoreCase(method)) Some(ctx)
    else None
}

/**
 * Matches HTTP requests with a method set to `GET`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case GET(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object GET extends Method("GET")

/**
 * Matches HTTP requests with a method set to `POST`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case POST(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object POST extends Method("POST")

/**
 * Matches HTTP requests with a method set to `PUT`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case PUT(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object PUT extends Method("PUT")

/**
 * Matches HTTP requests with a method set to `DELETE`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case DELETE(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object DELETE extends Method("DELETE")

/**
 * Matches HTTP requests with a method set to `HEAD`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case HEAD(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object HEAD extends Method("HEAD")

/**
 * Matches HTTP requests with a method set to `CONNECT`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case CONNECT(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object CONNECT extends Method("CONNECT")

/**
 * Matches HTTP requests with a method set to `OPTIONS`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case OPTIONS(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object OPTIONS extends Method("OPTIONS")

/**
 * Matches HTTP requests with a method set to `TRACE`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case TRACE(ctx) => {
 *       ...
 *     }
 *   })
 * }}}
 */
object TRACE extends Method("TRACE")

/**
 * Matches HTTP requests with the same case-sensitive path.
 *
 * For example, to match `/folderX` use:
 * {{{
 *   val r = Routes({
 *     case Path("/folderX") => {
 *       ...
 *     }
 *   })
 * }}}
 *
 * This will match `/folderX` but not: `/folderx`, `/folderX/` or `/TheFolderX`
 */
object Path {
  def unapply(ctx: SockoEvent) = Some(ctx.endPoint.path)
  def apply(ctx: SockoEvent) = ctx.endPoint.path
}

/**
 * Matches HTTP requests with the same path segment pattern.
 *
 * For example, to match `/record/1`, use:
 * {{{
 *   val r = Routes({
 *     case PathSegments("record" :: id :: Nil) => {
 *       // id will be set to 1
 *       ...
 *     }
 *   })
 * }}}
 */
object PathSegments {
  def unapply(ctx: SockoEvent): Option[List[String]] = ctx.endPoint.path.split("/").toList match {
    case "" :: rest => Some(rest) // skip a leading slash
    case all => Some(all)
  }
}

/**
 * Matches HTTP requests that have paths matching the specified regular expression.
 *
 * For example, to match `/path/to/file`, first define your regular expression as an object:
 * {{{
 *    object MyPathRegex extends PathRegex("""/path/([a-z0-9]+)/([a-z0-9]+)""".r)
 * }}}
 *
 * Then, when defining your Route:
 * {{{
 *   val r = Routes({
 *     case MyPathRegex(m) => {
 *       assert(m.group(1) == "to")
 *       assert(m.group(2) == "file")
 *       ...
 *     }
 *   })
 * }}}
 */
class PathRegex(regex: Regex) {
  def unapply(ctx: SockoEvent) = regex.findFirstMatchIn(ctx.endPoint.path)
}

/**
 * Matches HTTP requests with the same case-sensitive host.
 *
 * For example, to match `www.sockoweb.com`, use:
 * {{{
 *   val r = Routes({
 *     case Host("www.sockoweb.com") => {
 *       ...
 *     }
 *   })
 * }}}
 *
 * This will match `www.sockoweb.com` but not: `www1.sockoweb.com`, `sockoweb.com` or `sockoweb.org`
 */
object Host {
  def unapply(ctx: SockoEvent) = Some(ctx.endPoint.host)
  def apply(ctx: SockoEvent) = ctx.endPoint.host
}

/**
 * Matches HTTP requests with the same host segment pattern.
 *
 * For example, to match `server1.sockoweb.com`, use:
 * {{{
 *   val r = Routes({
 *     case HostSegments(server :: "sockoweb" :: "com" :: Nil) => {
 *       // server will be set to server1
 *       ...
 *     }
 *   })
 * }}}
 */
object HostSegments {
  def unapply(ctx: SockoEvent): Option[List[String]] = Some(ctx.endPoint.host.split("""\.""").toList)
}

/**
 * Matches HTTP requests that have hosts matching the specified regular expression.
 *
 * For example, to match `www.sockoweb.com`, first define your regex as an object:
 * {{{
 *    object MyHostRegex extends HostRegex("""www\.([a-z]+)\.com""".r)
 * }}}
 *
 * Then, when defining your Route:
 * {{{
 *   val r = Routes({
 *     case MyHostRegex(m) => {
 *       assert(m.group(1) == "sockoweb")
 *       ...
 *     }
 *   })
 * }}}
 */
class HostRegex(regex: Regex) {
  def unapply(ctx: SockoEvent) = {
    regex.findFirstMatchIn(ctx.endPoint.host)
  }
}

/**
 * Matches HTTP requests that have identical query string
 *
 * For example, to match `http://www.sockoweb.org/do?action=save`:
 * {{{
 *   val r = Routes({
 *     case QueryString("action=save") => {
 *       ...
 *     }
 *   })
 * }}}
 */
object QueryString {
  def unapply(ctx: SockoEvent) = Some(ctx.endPoint.queryString)
  def apply(ctx: SockoEvent) = ctx.endPoint.queryString
}

/**
 * Matches HTTP requests that have a query string matching the specified regular expression.
 *
 * For example, to match `?name1=value1`, first define your regular expression as an object:
 * {{{
 *    object MyQueryStringRegex extends QueryStringRegex("""name1=([a-z0-9]+)""".r)
 * }}}
 *
 * Then, when defining your Route:
 * {{{
 *   val r = Routes({
 *     case MyQueryStringRegex(m) => {
 *       assert(m.group(1) == "value1")
 *       ...
 *     }
 *   })
 * }}}
 */
class QueryStringRegex(regex: Regex) {
  def unapply(ctx: SockoEvent) = {
    regex.findFirstMatchIn(ctx.endPoint.queryString)
  }
}

/**
 * Matches HTTP requests that have a query string field with the specified name.
 *
 * If a match is found, the value is returned. If there are more than one value, on the first value is returned.
 *
 * For example, to match `?name1=value1`, first define your match as an object:
 * {{{
 *    object MyQueryStringField extends QueryStringName("name1")
 * }}}
 *
 * Then, when defining your Route:
 * {{{
 *   val r = Routes({
 *     case MyQueryStringField(value) => {
 *       assert(value == "value1")
 *       ...
 *     }
 *   })
 * }}}
 */
class QueryStringField(name: String) {
  def unapply(ctx: SockoEvent) = {
    ctx.endPoint.getQueryString(name)
  }
}

/**
 * Concatenates 2 extractors in a case statement
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     ctx @ GET(Path("/mypath")) & QueryString("name1=value1") => {
 *       ...
 *     }
 *   })
 * }}}
 */
object & { def unapply[A](a: A) = Some(a, a) }

