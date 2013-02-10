//
// Copyright 2013 Vibul Imtarnasan, David Bolton and Socko contributors.
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
package org.mashupbots.socko

/**
 * Tool to watch for changes in the file system and trigger a build.
 * We use this tool when developing javascript applications. 
 * 
 * Quite often, we like to combine static javascript and/or html files. To do this, we
 * use an Apache Ant build file. To manually run the build every time after a file is changed
 * during the development process is tiresome.
 * 
 * The tools in this package helps watch for changes on the file system and will automatically
 * trigger your build for you.
 */
package object buildtools {

}