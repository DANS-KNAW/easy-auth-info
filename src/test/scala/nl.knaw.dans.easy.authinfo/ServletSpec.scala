/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy.authinfo

import java.util.UUID

import org.apache.commons.configuration.PropertiesConfiguration
import org.eclipse.jetty.http.HttpStatus._
import org.json4s.native.JsonMethods
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success, Try }
import scalaj.http.HttpResponse

class ServletSpec extends TestSupportFixture with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private class Wiring extends ApplicationWiring(new Configuration("", new PropertiesConfiguration() {
    // need a constructor without arguments for the mock, the constructor needs a valid property
    addProperty("bag-store.url", "http://localhost:20110/")
  }))
  private val uuid = UUID.randomUUID()
  private val wiring = mock[Wiring] // mocking at a low level to test the chain of error handling
  addServlet(new EasyAuthInfoServlet(new EasyAuthInfoApp(wiring)), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Auth Info Service running..."
      status shouldBe OK_200
    }
  }

  "get /:uuid/*" should "return json" in {
    wiring.loadDDM _ expects uuid once() returning Success(<ddm:DDM></ddm:DDM>)
    wiring.loadFilesXML _ expects uuid once() returning Success(
      <files>
        <file filepath="some.file">
          <accessibleToRights>KNOWN</accessibleToRights>
          <visibleToRights>KNOWN</visibleToRights>
        </file>
      </files>
    )
    get(s"$uuid/some.file") {
      status shouldBe OK_200
      Try(JsonMethods.parse(body)) shouldBe a[Success[_]] // content details are tested with FileItemSpec
    }
  }

  it should "report file not found" in {
    wiring.loadDDM _ expects uuid once() returning Success(<ddm:DDM></ddm:DDM>)
    wiring.loadFilesXML _ expects uuid once() returning Success(<files></files>)
    get(s"$uuid/some.file") {
      body shouldBe s"$uuid/some.file does not exist"
      status shouldBe NOT_FOUND_404
    }
  }

  it should "report invalid uuid" in {
    get("1-2-3-4-5-6/some.file") {
      body shouldBe "Invalid UUID string: 1-2-3-4-5-6"
      status shouldBe BAD_REQUEST_400
    }
  }

  it should "report missing path" in {
    get(s"$uuid/") {
      body shouldBe "file path is missing"
      status shouldBe BAD_REQUEST_400
    }
  }

  it should "report bag not found" in {
    wiring.loadFilesXML _ expects uuid once() returning httpException(s"Bag $uuid does not exist in BagStore")
    get(s"$uuid/some.file") {
      body shouldBe s"$uuid does not exist"
      status shouldBe NOT_FOUND_404
    }
  }

  it should "report invalid bag" in {
    wiring.loadFilesXML _ expects uuid once() returning httpException(s"File $uuid/metadata/files.xml does not exist in BagStore")
    get(s"$uuid/some.file") {
      body shouldBe "not expected exception"
      status shouldBe INTERNAL_SERVER_ERROR_500
    }
  }

  private def httpException(message: String, code: Int = 404) = {
    val headers = Map[String, String]("Status" -> s"$code")
    Failure(HttpStatusException(message, HttpResponse("", code, headers)))
  }
}