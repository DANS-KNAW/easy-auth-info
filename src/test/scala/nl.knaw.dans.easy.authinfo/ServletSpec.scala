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
import java.util.UUID

import nl.knaw.dans.easy.authinfo.components.RightsFor._
import nl.knaw.dans.easy.authinfo.components.{ Solr, SolrImpl }
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.{ SolrClient, SolrRequest, SolrResponse }
import org.apache.solr.common.params.SolrParams
import org.apache.solr.common.util.NamedList
import org.apache.solr.common.{ SolrDocument, SolrDocumentList }
import org.eclipse.jetty.http.HttpStatus._
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite
import org.json4s.native.JsonMethods.parse

import scala.util.{ Failure, Success }
import scala.xml.Elem
import scalaj.http.HttpResponse

class ServletSpec extends TestSupportFixture with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private val mockDocList = mock[SolrDocumentList]

  private val app = new EasyAuthInfoApp {
    // mocking at a low level to test the chain of error handling
    override val bagStore: BagStore = mock[BagStore]
    override lazy val configuration: Configuration = new Configuration("", new PropertiesConfiguration() {
      addProperty("bag-store.url", "http://localhost:20110/")
      addProperty("solr.url", "http://hostThatDoesNotExist")
    })
    override val solr: Solr = new SolrImpl() {
      override val solrClient: SolrClient = new SolrClient() {
        // can't use mock because SolrClient has a final method

        override def query(params: SolrParams): QueryResponse = new QueryResponse() {
          override def getResults: SolrDocumentList = mockDocList
        }

        override def close(): Unit = ()

        override def request(solrRequest: SolrRequest[_ <: SolrResponse], s: String): NamedList[AnyRef] =
          throw new Exception("not expected")
      }
    }
  }
  private val uuid = UUID.randomUUID()
  private val openAccessDDM: Elem =
    <ddm:DDM>
      <ddm:profile>
        <ddm:accessRights>OPEN_ACCESS</ddm:accessRights>
        <ddm:available>1992-07-30</ddm:available>
      </ddm:profile>
    </ddm:DDM>
  addServlet(new EasyAuthInfoServlet(app), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY Auth Info Service running..."
      status shouldBe OK_200
    }
  }

  "get /:uuid/*" should "return json" in {
    app.bagStore.loadBagInfo _ expects uuid once() returning Success(Map("EASY-User-Account" -> "someone"))
    app.bagStore.loadDDM _ expects uuid once() returning Success(openAccessDDM)
    app.bagStore.loadFilesXML _ expects uuid once() returning Success(
      <files>
        <file filepath="some.file">
          <accessibleToRights>{KNOWN}</accessibleToRights>
          <visibleToRights>{KNOWN}</visibleToRights>
        </file>
      </files>
    )
    get(s"$uuid/some.file") {
      body shouldBe
        s"""{
           |  "itemId":"$uuid/some.file",
           |  "owner":"someone",
           |  "dateAvailable":"1992-07-30",
           |  "accessibleTo":"KNOWN",
           |  "visibleTo":"KNOWN"
           |}""".stripMargin
      status shouldBe OK_200
      // variations are tested with FileRightsSpec
    }
  }

  it should "return values from the cache" in {
    mockDocList.isEmpty _ expects() returning false
    mockDocList.iterator _ expects() returning new util.Iterator[SolrDocument]() {
      override def hasNext: Boolean = true

      override def next(): SolrDocument = new SolrDocument() {
        addField("id", s"$uuid/some.file")
        addField("easy_owner", "someone")
        addField("easy_date_available", "1992-07-30")
        addField("easy_accessible_to", "KNOWN")
        addField("easy_visible_to", "ANONYMOUS")
      }
    }
    get(s"$uuid/some.file") {
      val expected = s"""{
         |  "itemId":"$uuid/some.file",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
      checkSameHashMaps(expected, parse(body))
      status shouldBe OK_200
    }
  }

  it should "report file not found" in {
    app.bagStore.loadFilesXML _ expects uuid once() returning Success(<files/>)
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
      body shouldBe "file path is empty"
      status shouldBe BAD_REQUEST_400
    }
  }

  it should "report bag not found" in {
    app.bagStore.loadFilesXML _ expects uuid once() returning httpException(s"Bag $uuid does not exist in BagStore")
    get(s"$uuid/some.file") {
      body shouldBe s"$uuid does not exist"
      status shouldBe NOT_FOUND_404
    }
  }

  it should "report depositor not found" in {
    app.bagStore.loadBagInfo _ expects uuid once() returning Success(Map.empty)
    app.bagStore.loadDDM _ expects uuid once() returning Success(openAccessDDM)
    app.bagStore.loadFilesXML _ expects uuid once() returning Success(<files><file filepath="some.file"/></files>)
    get(s"$uuid/some.file") { // TODO intercept logging to show difference with the next test?
      body shouldBe s"not expected exception"
      status shouldBe INTERNAL_SERVER_ERROR_500
    }
  }

  it should "report invalid bag" in {
    app.bagStore.loadFilesXML _ expects uuid once() returning httpException(s"File $uuid/metadata/files.xml does not exist in BagStore")
    get(s"$uuid/some.file") {
      body shouldBe "not expected exception"
      status shouldBe INTERNAL_SERVER_ERROR_500
    }
  }

  private def httpException(message: String, code: Int = 404) = {
    val headers = Map("Status" -> IndexedSeq(s"$code"))
    Failure(HttpStatusException(message, HttpResponse("", code, headers)))
  }
}
