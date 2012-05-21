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
package org.mashupbots.socko.examples.streaming

import java.util.Date

import org.mashupbots.socko.context.HttpRequestProcessingContext
import org.mashupbots.socko.utils.CharsetUtil

import akka.actor.Actor

/**
 * Streaming processor streams a greeting and stops.
 */
class StreamingProcessor extends Actor {
  def receive = {
    case request: HttpRequestProcessingContext =>
      
      request.writeChunkResponse("text/plain; charset=UTF-8")
      
      request.writeChunk(("Hello from Socko (" + new Date().toString + ")\n").getBytes(CharsetUtil.UTF_8))
      request.writeChunk("This is the second line in the second chunk\n".getBytes(CharsetUtil.UTF_8))
      request.writeChunk("This is the third line ... ".getBytes(CharsetUtil.UTF_8))
      request.writeChunk("split into 2 chunks.".getBytes(CharsetUtil.UTF_8), true)
            
      context.stop(self)
  }
}

