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

package com.stackmob.newman.test

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import scalaz._
import Scalaz._
import net.liftweb.json._
import com.stackmob.newman._
import com.stackmob.newman.dsl._
import java.net.URL
import com.stackmob.newman.request.HttpRequest
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import com.stackmob.newman.Constants.UTF8Charset
import org.specs2.matcher.Matcher
import net.liftweb.json.scalaz.JsonScalaz._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.Future

class BodySerializationSpecs extends Specification { override def is = s2"""

  BodySerializationSpecs

  The Newman DSL is intended to make it easy to construct and execute HTTP requests

  Serialization should
    serialize with a provided JSONR ${SerializationTest().serializesWithJSONW}
    serialize without a provided JSONR ${SerializationTest().serializesWithoutJSONW}
    serialize with a specific JSONR ${SerializationTest().serializesWithSpecificJSONW}
  Deserialization should
    deserialize with a provided JSONR ${DeserializationTest().deserializesWithJSONR}
    deserialize without a provided JSONR ${DeserializationTest().deserializesWithoutJSONR}
    deserialize with a specific JSONR ${DeserializationTest().deserializesWithSpecificJSONR}
    deserialize with an overriding JSONR ${DeserializationTest().deserializesWithReplacedJSONR}
  """

  protected val url = new URL("http://stackmob.com")
  import BodySerializationSpecs._

  trait Context extends BaseContext {
    def ensureSucceedsWithReader[T : JSONR : Manifest](req: HttpRequest, expected: T) = {
      req.block().bodyAs[T].map { body: T =>
        (body must beEqualTo(expected)): SpecsResult
      } valueOr { logAndFail(_) }
    }

    def ensureSucceedsAsCaseClass[T <: AnyRef: Manifest](req: HttpRequest, expected: T) = {
      req.block().bodyAsCaseClass[T].map { body: T =>
        (body must beEqualTo(expected)): SpecsResult
      } valueOr { logAndFail(_) }
    }

    def succeedWith[E, A](a: =>A) = validationWith[E, A](Success(a))

    private def validationWith[E, A](f: =>Validation[E, A]): Matcher[Validation[E, A]] = (v: Validation[E, A]) => {
      val expected = f
      (expected == v, v+" is a "+expected, v+" is not a "+expected)
    }
  }

  case class SerializationTest() extends Context {
    implicit val client = new DummyHttpClient
    private lazy val transformer = PUT(url)

    def serializesWithJSONW: SpecsResult = {
      val bodyObject = SomeClass("all", 4, "u")
      val resultantBody: Array[Byte] = transformer.setBody(bodyObject).body
      fromJSON[SomeClass](parse(new String(resultantBody))) must succeedWith(bodyObject)
    }

    def serializesWithoutJSONW: SpecsResult = {
      val bodyObject = ClassWithoutWriter(1, 2, "check")
      val resultantBody: String = new String(transformer.setBody(bodyObject).body, UTF8Charset)
      resultantBody must beEqualTo("{\"j\":1,\"k\":2,\"l\":\"check\"}")
    }

    def serializesWithSpecificJSONW: SpecsResult = {
      implicit val stringsWriter = new JSONW[SomeClass] {
        def write(obj: SomeClass): JValue = {
          JObject(
            JField("a", JString(obj.a)) ::
            JField("c", JString(obj.c)) ::
            Nil)
        }
      }

      val bodyObject = SomeClass("boyz", 2, "men")
      val resultantBody: String = new String(transformer.setBody(bodyObject).body, UTF8Charset)
      resultantBody must beEqualTo("{\"a\":\"boyz\",\"c\":\"men\"}")
    }
  }

  case class DeserializationTest() extends Context {

    private def getResponse(bodyString: String): HttpRequest = {
      val bodyBytes = bodyString.getBytes(UTF8Charset)
      implicit val client = new DummyHttpClient(Future.successful(HttpResponse(HttpResponseCode.Ok, Headers.empty, bodyBytes)))
      GET(url)
    }

    def deserializesWithJSONR: SpecsResult = {
      val bodyObject = SomeClass("boyz", 2, "men")
      ensureSucceedsWithReader(getResponse(compactRender(toJSON(bodyObject))), bodyObject)
    }

    def deserializesWithoutJSONR: SpecsResult = {
      val bodyObject = ClassWithoutReader(9.5, false)
      ensureSucceedsAsCaseClass(getResponse(compactRender(toJSON(bodyObject))), bodyObject)
    }

    def deserializesWithSpecificJSONR: SpecsResult = {
      val bodyObject = SomeClass("boyz", 2, "men")
      implicit val reader = new JSONR[JustStrings] {
        def read(json: JValue): Result[JustStrings] = {
          (field[String]("first")(json) |@|
            field[String]("second")(json)) {
            JustStrings(_, _)
          }
        }
      }
      ensureSucceedsWithReader(getResponse(compactRender(toJSON(bodyObject))), bodyObject)
    }

    def deserializesWithReplacedJSONR: SpecsResult = {
      val bodyObject = SomeClass("boyz", 2, "men")
      val newBodyObject = SomeClass("AAAboyz", 2, "men")
      implicit val reader = new JSONR[SomeClass] {
        def read(json: JValue): Result[SomeClass] = {
          (field[String]("a")(json) |@|
          field[Int]("b")(json) |@|
          field[String]("c")(json)) { (_, _, _) =>
            newBodyObject
          }
        }
      }
      ensureSucceedsWithReader(getResponse(compactRender(toJSON(bodyObject))), newBodyObject)
    }
  }

}

object BodySerializationSpecs {
  case class SomeClass(a: String, b: Int, c: String)

  object SomeClass {
    implicit val reader: JSONR[SomeClass] = new JSONR[SomeClass] {
      def read(json: JValue): Result[SomeClass] = {
        (field[String]("a")(json) |@|
        field[Int]("b")(json) |@|
        field[String]("c")(json)) {
          SomeClass(_, _, _)
        }
      }
    }

    implicit val writer: JSONW[SomeClass] = new JSONW[SomeClass] {
      def write(obj: SomeClass): JValue = {
        JObject(
          JField("a", JString(obj.a)) ::
          JField("b", JInt(obj.b)) ::
          JField("c", JString(obj.c)) ::
          Nil)
      }
    }
  }

  case class ClassWithoutReader(x: Double, y: Boolean)
  object ClassWithoutReader {
    implicit val writer: JSONW[ClassWithoutReader] = new JSONW[ClassWithoutReader] {
      def write(obj: ClassWithoutReader): JValue = {
        JObject(
          JField("x", JDouble(obj.x)) ::
          JField("y", JBool(obj.y)) ::
          Nil)
      }
    }
  }

  case class ClassWithoutWriter(j: Int, k: Int, l: String)
  object ClassWithoutWriter {
    implicit def reader: JSONR[ClassWithoutWriter] = new JSONR[ClassWithoutWriter] {
      def read(json: JValue): Result[ClassWithoutWriter] = {
        (field[Int]("j")(json) |@|
        field[Int]("k")(json) |@|
        field[String]("l")(json)) {
          ClassWithoutWriter(_, _, _)
        }
      }
    }
  }

  case class JustStrings(a: String, c: String)
}