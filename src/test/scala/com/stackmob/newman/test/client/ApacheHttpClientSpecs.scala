/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman.test.client

import org.specs2.Specification
import com.stackmob.newman._

class ApacheHttpClientSpecs extends Specification with ClientTests { override def is = s2"""

  ApacheHttpClientSpecs

  ApacheHttpClient is the HttpClient implementation that actually hits the internet

  get should work ${ClientTests(httpClient).get}
  post should work ${ClientTests(httpClient).post}
  put should work ${ClientTests(httpClient).put}
  delete should work ${ClientTests(httpClient).delete}
  head should work ${ClientTests(httpClient).head}
  """

  private def httpClient = new ApacheHttpClient
}
