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

import java.util

import nl.knaw.dans.easy.authinfo.components.RightsFor._
import nl.knaw.dans.easy.authinfo.components.{ AuthCache, AuthCacheWithSolr }
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.solr.client.solrj.response.{ QueryResponse, UpdateResponse }
import org.apache.solr.client.solrj.{ SolrClient, SolrRequest, SolrResponse }
import org.apache.solr.common.params.SolrParams
import org.apache.solr.common.util.NamedList
import org.apache.solr.common.{ SolrDocument, SolrDocumentList, SolrInputDocument }
import org.eclipse.jetty.http.HttpStatus._
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }
import scala.xml.Elem
import scalaj.http.HttpResponse

class ServletSpec extends TestSupportFixture with ServletFixture
  with ScalatraSuite
  with MockFactory {

  trait UpdateResponseValues {
    def status: Int

    def response: NamedList[AnyRef]
  }
  private val mockUpdateResponseValues = mock[UpdateResponseValues]
  private val mockDocList = mock[SolrDocumentList]

  private val app = new EasyAuthInfoApp {
    // mocking at a low level to test the chain of error handling
    override val bagStore: BagStore = mock[BagStore]
    override lazy val configuration: Configuration = new Configuration("", new PropertiesConfiguration() {
      addProperty("bag-store.url", "http://localhost:20110/")
      addProperty("solr.url", "http://hostThatDoesNotExist")
    })
    override val authCache: AuthCache = new AuthCacheWithSolr() {
      override val solrClient: SolrClient = new SolrClient() {
        // can't use mock because SolrClient has a final method

        override def query(params: SolrParams): QueryResponse = new QueryResponse() {
          override def getResults: SolrDocumentList = mockDocList
        }

        override def add(doc: SolrInputDocument): UpdateResponse = new UpdateResponse {

          override def getStatus: Int = mockUpdateResponseValues.status

          override def getResponse: NamedList[AnyRef] = mockUpdateResponseValues.response
        }

        override def close(): Unit = ()

        override def request(solrRequest: SolrRequest[_ <: SolrResponse], s: String): NamedList[AnyRef] =
          throw new Exception("not expected")
      }
    }
  }

  private def expectsDocIsNotFoundInCache = {
    mockDocList.isEmpty _ expects() once() returning true
  }

  private def expectsDocFoundInCahce(document: SolrDocument) = {
    val cachedDoc = new util.Iterator[SolrDocument]() {

      override def hasNext: Boolean = true

      override def next(): SolrDocument = {
        document
      }
    }
    mockDocList.isEmpty _ expects() once() returning false
    mockDocList.iterator _ expects() once() returning cachedDoc
  }

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
    </files>

  addServlet(new EasyAuthInfoServlet(app), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Auth Info Service running..."
      status shouldBe OK_200
    }
  }

  "get /:uuid/*" should "return values from the solr cache" in {
    expectsDocFoundInCahce(new SolrDocument() {
      addField("id", s"${ randomUUID }/some.file")
      addField("easy_owner", "someone")
      addField("easy_date_available", "1992-07-30")
      addField("easy_accessible_to", "KNOWN")
      addField("easy_visible_to", "ANONYMOUS")
    })
    get(s"${ randomUUID }/some.file") {
      // in this case the fields are returned in a random order
      body should include(s""""itemId":"${ randomUUID }/some.file"""")
      body should include(s""""owner":"someone"""")
      body should include(s""""dateAvailable":"1992-07-30"""")
      body should include(s""""accessibleTo":"KNOWN"""")
      body should include(s""""visibleTo":"ANONYMOUS"""")
      status shouldBe OK_200
    }
  }
  it should "report cache was updated" in {
    expectsDocIsNotFoundInCache
    mockUpdateResponseValues.status _ expects() once() returning 0
    app.bagStore.loadBagInfo _ expects randomUUID once() returning Success(Map("EASY-User-Account" -> "someone"))
    app.bagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(FilesWithAllRightsForKnown)
    shouldReturn(OK_200,
      s"""{
         |  "itemId":"${ randomUUID }/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"KNOWN"
         |}""".stripMargin
    ) // variations are tested with FileRightsSpec
  }

  it should "report cache update failed" in {
    expectsDocIsNotFoundInCache
    mockUpdateResponseValues.status _ expects() once() returning -1
    mockUpdateResponseValues.response _ expects() once() returning new NamedList[AnyRef]()
    app.bagStore.loadBagInfo _ expects randomUUID once() returning Success(Map("EASY-User-Account" -> "someone"))
    app.bagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(FilesWithAllRightsForKnown)
    shouldReturn(OK_200,
      s"""{
         |  "itemId":"${ randomUUID }/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"KNOWN"
         |}""".stripMargin
    ) // variations are tested with FileRightsSpec
  }

  it should "report invalid uuid" in {
    shouldReturn(BAD_REQUEST_400, "Invalid UUID string: 1-2-3-4-5-6", whenRequesting = "1-2-3-4-5-6/some.file")
  }

  it should "report missing path" in {
    shouldReturn(BAD_REQUEST_400, "file path is empty", whenRequesting = s"${ randomUUID }/")
  }

  it should "report bag not found" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadFilesXML _ expects randomUUID once() returning httpException(s"Bag ${ randomUUID } does not exist in BagStore")
    shouldReturn(NOT_FOUND_404, s"${ randomUUID } does not exist")
  }

  it should "report file not found" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(<files/>)
    shouldReturn(NOT_FOUND_404, s"${ randomUUID }/some.file does not exist")
  }

  it should "report invalid bag: no files.xml" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadFilesXML _ expects randomUUID once() returning httpException(s"File ${ randomUUID }/metadata/files.xml does not exist in BagStore")
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no DDM" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    app.bagStore.loadDDM _ expects randomUUID once() returning httpException(s"File ${ randomUUID }/metadata/dataset.xml does not exist in BagStore")
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no profile in DDM" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    app.bagStore.loadDDM _ expects randomUUID once() returning Success(<ddm:DDM/>)
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no date available in DDM" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    app.bagStore.loadDDM _ expects randomUUID once() returning Success(<ddm:DDM><ddm:profile/></ddm:DDM>)
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: no bag-info.txt" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    app.bagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    app.bagStore.loadBagInfo _ expects randomUUID once() returning httpException(s"File ${ randomUUID }/info.txt does not exist in BagStore")
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  it should "report invalid bag: depositor not found" in {
    expectsDocIsNotFoundInCache
    app.bagStore.loadBagInfo _ expects randomUUID once() returning Success(Map.empty)
    app.bagStore.loadDDM _ expects randomUUID once() returning Success(openAccessDDM)
    app.bagStore.loadFilesXML _ expects randomUUID once() returning Success(<files><file filepath="some.file"/></files>)
    shouldReturn(INTERNAL_SERVER_ERROR_500, s"not expected exception")
  }

  private def shouldReturn(expectedStatus: Int, expectedBody: String, whenRequesting: String = s"${ randomUUID }/some.file"): Any = {
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
