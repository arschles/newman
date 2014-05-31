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

import com.stackmob.newman.{Headers, Constants, HttpClient}
import com.stackmob.newman.dsl._
import com.stackmob.newman.request.HttpRequest
import com.stackmob.newman.response.HttpResponseCode
import java.net.URL
import scala.concurrent.duration.Duration
import com.stackmob.newman.dsl.transformerToHttpRequest
import com.stackmob.newman.test._
import com.stackmob.newman.FinagleHttpClient.RichRawBody
import org.specs2.matcher.{MustMatchers, MustExpectations}
import org.specs2.Specification

trait ClientTests extends MustExpectations with MustMatchers with ResponseMatcher with HeadersMatcher { this: Specification =>

  implicit private val charset = Constants.UTF8Charset

  class ClientTests(implicit client: HttpClient) {

    private def execute(req: HttpRequest,
                        expectedCode: HttpResponseCode = HttpResponseCode.Ok,
                        expectedHeaders: Headers = Headers("Content-Type" -> "application/json"),
                        duration: Duration = duration,
                        checkResponseBody: Boolean = true)
                       (expectedResponseChecker: ExpectedResponseChecker) = {
      val resp = req.apply.block(duration)
      resp must beResponse(expectedCode, expectedHeaders, checkResponseBody)(expectedResponseChecker)
    }

    private lazy val headerTup = "X-Stackmob-Test-Header" -> "X-StackMob-Test-Value"
    //part of the JSON body should contain the header, in this format
    private lazy val headers = Headers(headerTup)

    private final val base = "http://gohttpbin.appspot.com"
    private lazy val getURL = new URL(s"$base/get")
    private lazy val postURL = new URL(s"$base/post")
    private lazy val putURL = new URL(s"$base/put")
    private lazy val deleteURL = new URL(s"$base/delete")
    private lazy val headURL = new URL(s"$base/head")

    private lazy val bodyString = "StackMobTestBody"
    private lazy val body = bodyString.getBytes(charset)

    def get = {
      execute(GET(getURL).addHeaders(headers)) { resp =>
        resp.headers must containHeaders(headers)
      }
    }

    def post = {
      execute(POST(postURL).addBody(body).addHeaders(headers)) { resp =>
        val headersRes = resp.headers.toList.mkString(",") must contain(List(headerTup).mkString(","))
        val dataRes = resp.mbData must beSome.like {
          case s => s must beEqualTo(body.stringRepresentation)
        }
        headersRes and dataRes
      }
    }

    def put = {
      execute(PUT(putURL).addBody(body).addHeaders(headers)) { resp =>
        val headersRes = resp.headers must containHeaders(headers)
        val dataRes = resp.mbData must beSome.like {
          case s => s must beEqualTo(body.stringRepresentation)
        }
        headersRes and dataRes
      }
    }

    def delete = {
      execute(DELETE(deleteURL).addHeaders(headers)) { resp =>
        resp.headers must containHeaders(headers)
      }
    }

    def head = {
      execute(HEAD(headURL).addHeaders(headers), checkResponseBody = false) { resp =>
        //will not be executed
        true must beTrue
      }
    }
  }

  object ClientTests {
    def apply(client: HttpClient) = {
      new ClientTests()(client)
    }
  }

}

