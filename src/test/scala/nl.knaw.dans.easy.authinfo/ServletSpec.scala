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
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }
import scala.xml.Elem
import scalaj.http.HttpResponse

class ServletSpec extends TestSupportFixture with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private class Wiring extends ApplicationWiring(new Configuration("", new PropertiesConfiguration() {
    addProperty("bag-store.url", "http://localhost:20110/")
  }))
  private val wiring = mock[Wiring]
  private val app = new EasyAuthInfoApp(wiring)
  addServlet(new EasyAuthInfoServlet(app), "/*")

  private val uuid = UUID.randomUUID()
  private val ddmOpenAccess: Elem = <ddm:DDM><ddm:profile><ddm:accessRights>OPEN_ACCESS</ddm:accessRights></ddm:profile></ddm:DDM>

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Auth Info Service running..."
      status shouldBe OK_200
    }
  }

  "get /:uuid/*" should "return json" in {
    wiring.loadDDM _ expects uuid once() returning Success(ddmOpenAccess)
    wiring.loadFilesXML _ expects uuid once() returning Success(<files><file filepath="some.file"></file></files>)
    get(s"$uuid/some.file") {
      body shouldBe """{
                      |  "accessibleTo":"ANONYMOUS",
                      |  "visibleTo":"ANONYMOUS"
                      |}""".stripMargin
      status shouldBe OK_200
    }
  }

  it should "report file not found" in {
    wiring.loadDDM _ expects uuid once() returning Success(ddmOpenAccess)
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
    wiring.loadFilesXML _ expects uuid once() returning Failure(createHttpException(s"Bag $uuid does not exist in BagStore", 404))
    get(s"$uuid/some.file") {
      body shouldBe s"$uuid does not exist"
      status shouldBe NOT_FOUND_404
    }
  }

  it should "report invalid bag" in {
    wiring.loadFilesXML _ expects uuid once() returning Failure(createHttpException(s"File $uuid/metadata/files.xml does not exist in BagStore", 404))
    get(s"$uuid/some.file") {
      body shouldBe "not expected exception"
      status shouldBe INTERNAL_SERVER_ERROR_500
    }
  }

  private def createHttpException(msg: String, code: Int) = {
    HttpStatusException(msg, HttpResponse("", code, Map[String, String]("Status" -> s"$code")))
  }
}