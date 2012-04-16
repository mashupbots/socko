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

import org.mashupbots.socko.context.ProcessingContext

/**
 * Routes define the rules for dispatching requests to its intended Akka actor processors. It is implemented as a
 * list of PartialFunctions.
 *
 * To assist with routing, use the following extractors:
 *  - HTTP Method: GET, POST, PUT, DELETE, HEAD, CONNECT, OPTIONS, TRACE
 *  - HTTP Path: Path, PathSegments, PathRegex
 *  - HTTP Host: Host, HostSegments, HostRegex
 *  - HTTP Query String: QueryString, QueryStringRegex
 *
 * Example of a single list of partial functions:
 * {{{
 *   val r = Routes({
 *     case ctx @ GET(Path(PathSegments("record" :: id :: Nil))) => {
 *       ...
 *     }
 *     case ctx @ Path(PathSegments("record" :: id :: Nil)) => {
 *       ...
 *     }
 *   })
 * }}}
 *
 * Example of 2 lists of partial functions:
 * {{{
 *   val r = Routes(
 *     {
 *       case ctx @ GET(Path(PathSegments("record" :: id :: Nil))) => {
 *         ...
 *       }
 *       case ctx @ Path(PathSegments("record" :: id :: Nil)) => {
 *         ...
 *       }
 *     },
 *     {
 *       case ctx @ PUT(Host("aaa.abc.com")) & Path("/test1") => {
 *         ...
 *       }
 *       case ctx @ Host("aaa.abc.com") & Path("/test2") => {
 *         ...
 *       }
 *     })
 * }}}
 *
 * This area of code uses extractors or, the `unapply` method. It is important to understand
 * how to use these patterns.
 *  - `ctx @` is scala's variable binding pattern. It will be assigned the value of the matching ProcessingContext
 *  - `GET(x)` and `Path(y)` are extractors. In the case of `GET, `x` is the same ProcessingContext as `ctx`. For `Path`,
 *     `y` is the path as a string. In other words, the value in between the parentheses is the return value of the
 *     associated `unapply` method.
 *  - `&` chains together extractors like a logical AND
 *
 * This [[http://daily-scala.blogspot.com.au/2010/02/chaining-partial-functions-with-orelse.html article]]
 * explains how chaining of `PartialFunction` works using `orElse`.
 *
 */
object Routes {
  def apply(funcList: PartialFunction[ProcessingContext, Unit]*) = {
    funcList.toList.reduceLeft { (functions, f) => functions orElse f }
  }
}

/**
 * Used to help match the HTTP method.
 *
 * You should not need to use this class. Rather use the objects that extends from this class. For example:
 * [[org.mashupbots.socko.routes.GET]].
 *
 * @param method HTTP Method
 */
class Method(method: String) {
  def unapply(ctx: ProcessingContext) =
    if (ctx.endPoint.method.equalsIgnoreCase(method)) Some(ctx)
    else None
}

/**
 * Matches HTTP requests with a method set to `GET`.
 *
 * For example:
 * {{{
 *   val r = Routes({
 *     case ctx @ GET(_) => {
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
 *     case ctx @ POST(_) => {
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
 *     case ctx @ PUT(_) => {
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
 *     case ctx @ DELETE(_) => {
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
 *     case ctx @ HEAD(_) => {
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
 *     case ctx @ CONNECT(_) => {
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
 *     case ctx @ OPTIONS(_) => {
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
 *     case ctx @ TRACE(_) => {
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
 *     case ctx @ Path("/folderX") => {
 *       ...
 *     }
 *   })
 * }}}
 *
 * This will match `/folderX` but not: `/folderx`, `/folderX/` or `/TheFolderX`
 */
object Path {
  def unapply(ctx: ProcessingContext) = Some(ctx.endPoint.path)
  def apply(ctx: ProcessingContext) = ctx.endPoint.path
}

/**
 * Matches HTTP requests with the same path segment pattern.
 *
 * For example, to match `/record/1`, use:
 * {{{
 *   val r = Routes({
 *     case ctx @ Path(PathSegments("record" :: id :: Nil)) => {
 *       // id will be set to 1
 *       ...
 *     }
 *   })
 * }}}
 */
object PathSegments {
  def unapply(path: String): Option[List[String]] = path.split("/").toList match {
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
 *     case ctx @ case MyPathRegex(m) => {
 *       assert(m.group(1) == "to")
 *       assert(m.group(2) == "file")
 *       ...
 *     }
 *   })
 * }}}
 */
class PathRegex(regex: Regex) {
  def unapply(ctx: ProcessingContext) = regex.findFirstMatchIn(ctx.endPoint.path)
}

/**
 * Matches HTTP requests with the same case-sensitive host.
 *
 * For example, to match `www.sockoweb.com`, use:
 * {{{
 *   val r = Routes({
 *     case ctx @ Host("www.sockoweb.com") => {
 *       ...
 *     }
 *   })
 * }}}
 *
 * This will match `www.sockoweb.com` but not: `www1.sockoweb.com`, `sockoweb.com` or `sockoweb.org`
 */
object Host {
  def unapply(ctx: ProcessingContext) = Some(ctx.endPoint.host)
  def apply(ctx: ProcessingContext) = ctx.endPoint.host
}

/**
 * Matches HTTP requests with the same host segment pattern.
 *
 * For example, to match `server1.sockoweb.com`, use:
 * {{{
 *   val r = Routes({
 *     case ctx @ Host(HostSegments(server :: "sockoweb" :: "com" :: Nil)) => {
 *       // server will be set to server1
 *       ...
 *     }
 *   })
 * }}}
 */
object HostSegments {
  def unapply(host: String): Option[List[String]] = Some(host.split("""\.""").toList)
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
 *     case ctx @ MyHostRegex(m) => {
 *       assert(m.group(1) == "sockoweb")
 *       ...
 *     }
 *   })
 * }}}
 */
class HostRegex(regex: Regex) {
  def unapply(ctx: ProcessingContext) = {
    regex.findFirstMatchIn(ctx.endPoint.host)
  }
}

/**
 * Matches HTTP requests that have identical query string
 *
 * For example, to match `http://www.sockoweb.org/do?action=save`:
 * {{{
 *   val r = Routes({
 *     case ctx @ QueryString("action=save") => {
 *       ...
 *     }
 *   })
 * }}}
 */
object QueryString {
  def unapply(ctx: ProcessingContext) = Some(ctx.endPoint.queryString)
  def apply(ctx: ProcessingContext) = ctx.endPoint.queryString
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
 *     case ctx @ MyQueryStringRegex(m) => {
 *       assert(m.group(1) == "value1")
 *       ...
 *     }
 *   })
 * }}}
 */
class QueryStringRegex(regex: Regex) {
  def unapply(ctx: ProcessingContext) = {
    regex.findFirstMatchIn(ctx.endPoint.queryString)
  }
}

/**
 * Matches HTTP requests that have a query string item with the specified name.
 *
 * If a match is found, the value is returned.  If there are more than one value, on the first value is returned.
 *
 * For example, to match `?name1=value1`, first define your match as an object:
 * {{{
 *    object MyQueryStringName extends QueryStringName("name1")
 * }}}
 *
 * Then, when defining your Route:
 * {{{
 *   val r = Routes({
 *     case ctx @ MyQueryStringName(value) => {
 *       assert(value == "value1")
 *       ...
 *     }
 *   })
 * }}}
 */
class QueryStringMatcher(name: String) {
  def unapply(ctx: ProcessingContext) = {
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

