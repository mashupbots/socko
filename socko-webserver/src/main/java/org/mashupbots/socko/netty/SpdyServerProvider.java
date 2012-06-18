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
package org.mashupbots.socko.netty;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.npn.NextProtoNego.ServerProvider;

/**
 * Provider for Jetty's `NextProtoNego` 
 */
public class SpdyServerProvider implements ServerProvider {

	private String selectedProtocol = null;

	public void unsupported() {
		// if unsupported, default to http/1.1
		selectedProtocol = "http/1.1";
	}

	public List<String> protocols() {
		return Arrays.asList("spdy/3", "spdy/2", "http/1.1");
	}

	public void protocolSelected(String protocol) {
		selectedProtocol = protocol;
	}

	public String getSelectedProtocol() {
		return selectedProtocol;
	}
}
