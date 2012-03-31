//
// Copyright 2012 Vibul Imtarnasan and David Bolton.
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
package org.mashupbots.socko.handler

import org.mashupbots.socko.context.ProcessingContext
import scala.util.matching.Regex

/**
 * A list of PartialFunctions used for routing HTTP requests.
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
 *       result = "1"
 *     }
 *     case ctx @ Path(PathSegments("record" :: id :: Nil)) => {
 *       // Get storing data in our cache
 *       ctx.cache.put("id", id)
 *       result = "2"
 *     }
 *   })
 * }}}
 *
 * Example of 2 lists of partial functions:
 * {{{
 *   val r = Routes(
 *     {
 *       case ctx @ GET(Path(PathSegments("record" :: id :: Nil))) => {
 *         result = "1"
 *       }
 *       case ctx @ Path(PathSegments("record" :: id :: Nil)) => {
 *         // Get storing data in our cache
 *         ctx.cache.put("id", id)
 *         result = "2"
 *       }
 *     },
 *     {
 *       case ctx @ PUT(Host("aaa.abc.com")) & Path("/test1") => result = "1"
 *       case ctx @ Host("aaa.abc.com") & Path("/test2") => result = "2"
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
 * HTTP method routing
 *
 * @param method HTTP Method
 */
class Method(method: String) {
  def unapply(ctx: ProcessingContext) =
    if (ctx.endPoint.method.equalsIgnoreCase(method)) Some(ctx)
    else None
}

object GET extends Method("GET")
object POST extends Method("POST")
object PUT extends Method("PUT")
object DELETE extends Method("DELETE")
object HEAD extends Method("HEAD")
object CONNECT extends Method("CONNECT")
object OPTIONS extends Method("OPTIONS")
object TRACE extends Method("TRACE")

/**
 * HTTP path routing. Must be exact match.
 *
 * For example:
 * {{{
 *   case Path("/test1")
 * }}}
 */
object Path {
  def unapply(ctx: ProcessingContext) = Some(ctx.endPoint.path)
  def apply(ctx: ProcessingContext) = ctx.endPoint.path
}

/**
 * HTTP path segments routing.
 *
 * For example, to match `/record/1`, use:
 * {{{
 *   case ctx @ Path(PathSegments("record" :: id :: Nil)) => {
 *     // id will be set to 1
 *     ctx.cache.put("id", id)
 *   }
 * }}}
 */
object PathSegments {
  def unapply(path: String): Option[List[String]] = path.split("/").toList match {
    case "" :: rest => Some(rest) // skip a leading slash
    case all => Some(all)
  }
}

/**
 * Regular expression matching of HTTP path routing
 *
 * For example, to match `/path/to/file`, first define your regular expression as an object:
 * {{{
 *    object MyPathRegex extends PathRegex("""/path/([a-z0-9]+)/([a-z0-9]+)""".r)
 * }}}
 *
 * Then, when defining your Route:
 * {{{
 *   case ctx @ case MyPathRegex(m) => {
 *     assert(m.group(1) == "to")
 *     assert(m.group(2) == "file")
 *   }
 * }}}
 */
class PathRegex(regex: Regex) {
  def unapply(ctx: ProcessingContext) = regex.findFirstMatchIn(ctx.endPoint.path)
}

/**
 * HTTP host routing. Must be exact match.
 *
 * For example, to match `www.abc.com`, use:
 * {{{
 *   case Host("www.abc.com")
 * }}}
 */
object Host {
  def unapply(ctx: ProcessingContext) = Some(ctx.endPoint.host)
  def apply(ctx: ProcessingContext) = ctx.endPoint.host
}

/**
 * HTTP host segments for routing
 *
 * For example, to match `server1.abc.com`, use:
 * {{{
 *   case ctx @ Host(HostSegments(server :: "abc" :: "com" :: Nil)) => {
 *     // server will be set to server1
 *     ctx.cache.put("server", server)
 *   }
 * }}}
 */
object HostSegments {
  def unapply(host: String): Option[List[String]] = Some(host.split("""\.""").toList)
}

/**
 * Regular expression matching of HTTP host routing
 *
 * For example, to match `www.abc.com`, first define your regex as an object:
 * {{{
 *    object MyHostRegex extends HostRegex("""www\.([a-z]+)\.com""".r)
 * }}}
 *
 * Then, when defining your Route:
 * {{{
 *   case ctx @ case MyHostRegex(m) => {
 *     assert(m.group(1) == "abc")
 *   }
 * }}}
 */
class HostRegex(regex: Regex) {
  def unapply(ctx: ProcessingContext) = {
    regex.findFirstMatchIn(ctx.endPoint.host)
  }
}

/**
 * Concatenates 2 extractors in a case statement
 */
object & { def unapply[A](a: A) = Some(a, a) }

