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

package com.stackmob.newman
package serialization.response

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import java.util.Date
import com.stackmob.newman.Constants._
import com.stackmob.newman.serialization.common._
import java.nio.charset.Charset

class HttpResponseSerialization(charset: Charset = UTF8Charset) extends SerializationBase[HttpResponse] {
  protected val CodeKey = "code"
  protected val HeadersKey = "headers"
  protected val BodyKey = "body"
  protected val TimeReceivedKey = "time_received"

  override implicit lazy val writer = new JSONW[HttpResponse] {

    import HeadersSerialization.{writer => HeadersWriter}
    import HttpResponseCodeSerialization.{writer => ResponseCodeWriter}

    override def write(h: HttpResponse): JValue = {
      JObject(
        JField(CodeKey, toJSON(h.code)(ResponseCodeWriter)) ::
        JField(HeadersKey, toJSON(h.headers)(HeadersWriter)) ::
        JField(BodyKey, JString(h.bodyString(charset))) ::
        JField(TimeReceivedKey, JInt(h.timeReceived.getTime)) ::
        Nil
      )
    }
  }

  override implicit lazy val reader = new JSONR[HttpResponse] {

    import HeadersSerialization.{reader => HeadersReader}
    import HttpResponseCodeSerialization.{reader => ResponseCodeReader}

    override def read(json: JValue): Result[HttpResponse] = {
      val codeField = field[HttpResponseCode](CodeKey)(json)(ResponseCodeReader)
      val headersField = field[Headers](HeadersKey)(json)(HeadersReader)
      val bodyField = field[String](BodyKey)(json)
      val timeReceivedField = field[Long](TimeReceivedKey)(json)

      (codeField |@| headersField |@| bodyField |@| timeReceivedField) {
        (code: HttpResponseCode, headers: Headers, body: String, timeReceivedMilliseconds: Long) =>
          HttpResponse(code, headers, body.getBytes(charset), new Date(timeReceivedMilliseconds))
      }
    }
  }
}
