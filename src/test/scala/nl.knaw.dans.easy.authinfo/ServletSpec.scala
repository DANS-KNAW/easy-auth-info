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

import nl.knaw.dans.easy.authinfo.components.RightsFor._
import nl.knaw.dans.easy.authinfo.components.SolrMocker._
import org.apache.solr.common.SolrDocument
import org.eclipse.jetty.http.HttpStatus._
import org.scalatra.test.EmbeddedJettyContainer
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }
import scala.xml.Elem
import scalaj.http.HttpResponse

class ServletSpec extends TestSupportFixture with EmbeddedJettyContainer with ScalatraSuite {

  private val app = mockApp
  private val mockedBagStore: app.BagStore = app.bagStore
  addServlet(new EasyAuthInfoServlet(app), "/*")

  private val openAccessDDM: Elem =
    <ddm:DDM>
      <ddm:profile>
        <ddm:accessRights>OPEN_ACCESS</ddm:accessRights>
        <ddm:available>1992-07-30</ddm:available>
      </ddm:profile>
    </ddm:DDM>

  private val FilesWithAllRightsForKnown: Elem =
    <files>
      <file filepath="some.file">
        <accessibleToRights>{KNOWN}</accessibleToRights>
        <visibleToRights>{KNOWN}</visibleToRights>
      </file>
      <file filepath="path/to/some.file">
        <accessibleToRights>{KNOWN}</accessibleToRights>
        <visibleToRights>{KNOWN}</visibleToRights>
      </file>
    </files>

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Auth Info Service running..."
      status shouldBe OK_200
    }
  }

  "get /:uuid/*" should "return values from the solr cache" in {
    expectsSolrDocInCache(new SolrDocument() {
      addField("id", s"$randomUUID/some%2Efile")
      addField("easy_owner", "someone")
      addField("easy_date_available", "1992-07-30")
      addField("easy_accessible_to", "KNOWN")
      addField("easy_visible_to", "ANONYMOUS")
    })
    get(s"$randomUUID/some.file") {
      // in this case the fields are returned in a random order
      body should include(s""""itemId":"$randomUUID/some%2Efile"""")
      body should include(s""""owner":"someone"""")
      body should include(s""""dateAvailable":"1992-07-30"""")
      body should include(s""""accessibleTo":"KNOWN"""")
      body should include(s""""visibleTo":"ANONYMOUS"""")
      status shouldBe OK_200
    }
  }

  it should "report cache was updated" in {
    expectsSolrDocIsNotInCache
    expectsSolrDocUpdateSuccess
    mockedBagStore.loadBagInfo _ expects randomUUID once() returning Success(Map("EASY-User-Account" -> "someone"))
    mockedBagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(FilesWithAllRightsForKnown)
    shouldReturn(OK_200,
      s"""{
         |  "itemId":"$randomUUID/path/to/some%2Efile",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"KNOWN"
         |}""".stripMargin,
      whenRequesting = s"$randomUUID/path/to/some.file"
    ) // variations are tested with FileRightsSpec
  }

  it should "report cache update failed" in {
    expectsSolrDocIsNotInCache
    expextsSolrDocUpdateFailure
    mockedBagStore.loadBagInfo _ expects randomUUID once() returning Success(Map("EASY-User-Account" -> "someone"))
    mockedBagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(FilesWithAllRightsForKnown)
    shouldReturn(OK_200,
      s"""{
         |  "itemId":"$randomUUID/some%2Efile",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"KNOWN"
         |}""".stripMargin
    )
  }

  it should "report invalid uuid" in {
    shouldReturn(BAD_REQUEST_400, "Invalid UUID string: 1-2-3-4-5-6", whenRequesting = "1-2-3-4-5-6/some.file")
  }

  it should "report missing path" in {
    shouldReturn(BAD_REQUEST_400, "file path is empty", whenRequesting = s"$randomUUID/")
  }

  it should "report bag not found" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Failure(BagDoesNotExistException(randomUUID))
    shouldReturn(NOT_FOUND_404, s"$randomUUID/some%2Efile does not exist")
  }

  it should "report file not found" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(<files/>)
    shouldReturn(NOT_FOUND_404, s"$randomUUID/some%2Efile does not exist")
  }

  it should "report invalid bag: no files.xml" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning httpException(s"File $randomUUID/metadata/files.xml does not exist in BagStore")
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no DDM" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    mockedBagStore.loadDDM _ expects randomUUID once() returning httpException(s"File $randomUUID/metadata/dataset.xml does not exist in BagStore")
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no profile in DDM" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    mockedBagStore.loadDDM _ expects randomUUID once() returning Success(<ddm:DDM/>)
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no date available in DDM" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    mockedBagStore.loadDDM _ expects randomUUID once() returning Success(<ddm:DDM><ddm:profile/></ddm:DDM>)
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no bag-info.txt" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    mockedBagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    mockedBagStore.loadBagInfo _ expects randomUUID once() returning httpException(s"File $randomUUID/info.txt does not exist in BagStore")
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: depositor not found" in {
    expectsSolrDocIsNotInCache
    mockedBagStore.loadBagInfo _ expects randomUUID once() returning Success(Map.empty)
    mockedBagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    mockedBagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  private def shouldReturn(expectedStatus: Int, expectedBody: String, whenRequesting: String = s"$randomUUID/some.file"): Any = {
    // verify logging manually: set log-level on warn in test/resources/logback.xml //TODO? file appender for testDir/XxxSpec/app.log
    get(whenRequesting) {
      body shouldBe expectedBody
      status shouldBe expectedStatus
    }
  }

  private def httpException(message: String, code: Int = 404) = {
    val headers = Map("Status" -> IndexedSeq(s"$code"))
    Failure(HttpStatusException(message, HttpResponse("", code, headers)))
  }
}
