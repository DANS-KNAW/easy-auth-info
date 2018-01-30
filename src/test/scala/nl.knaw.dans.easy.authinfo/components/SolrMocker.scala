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
package nl.knaw.dans.easy.authinfo.components

import java.util

import org.apache.solr.client.solrj.{ SolrClient, SolrRequest, SolrResponse }
import org.apache.solr.client.solrj.response.{ QueryResponse, UpdateResponse }
import org.apache.solr.common.{ SolrDocument, SolrDocumentList, SolrInputDocument }
import org.apache.solr.common.params.SolrParams
import org.apache.solr.common.util.NamedList
import org.scalamock.scalatest.MockFactory

object SolrMocker extends MockFactory {
  trait UpdateResponseValues {
    def status: Int

    def response: NamedList[AnyRef]
  }
  private val mockUpdateResponseValues = mock[UpdateResponseValues]
  private val mockDocList = mock[SolrDocumentList]

  val mockedSolrClient: SolrClient = new SolrClient() {
    // can't use mock because SolrClient has a final method
    // override the abstract and expected methods, most not expected calls are supposed to fail

    override def query(params: SolrParams): QueryResponse = new QueryResponse() {
      override def getResults: SolrDocumentList = mockDocList
    }

    override def add(doc: SolrInputDocument, commitWithinMs: Int): UpdateResponse = new UpdateResponse {

      override def getStatus: Int = mockUpdateResponseValues.status

      override def getResponse: NamedList[AnyRef] = mockUpdateResponseValues.response
    }

    override def close(): Unit = ()

    override def request(solrRequest: SolrRequest[_ <: SolrResponse], s: String): NamedList[AnyRef] =
      throw new Exception("not expected")
  }

  def expectsSolrDocIsNotInCache: Any = {
    mockDocList.isEmpty _ expects() once() returning true
  }

  def expectsSolrDocInCahce(document: SolrDocument): Any = {
    val cachedDoc = new util.Iterator[SolrDocument]() {

      override def hasNext: Boolean = true

      override def next(): SolrDocument = {
        document
      }
    }
    mockDocList.isEmpty _ expects() once() returning false
    mockDocList.iterator _ expects() once() returning cachedDoc
  }

  def expectsSolrDocUpdateSuccess: Any = {
    mockUpdateResponseValues.status _ expects() once() returning 0
  }

  def expextsSolrDocUpdateFailure: Any = {
    mockUpdateResponseValues.status _ expects() once() returning -1
    mockUpdateResponseValues.response _ expects() once() returning new NamedList[AnyRef]()
  }
}

