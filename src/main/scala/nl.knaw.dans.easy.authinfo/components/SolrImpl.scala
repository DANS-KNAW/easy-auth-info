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

import nl.knaw.dans.easy.authinfo.components.Solr.SolrLiterals
import nl.knaw.dans.easy.authinfo.{ SolrBadRequestException, SolrCommitException, SolrDeleteException, SolrSearchException, SolrStatusException, SolrUpdateException }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.http.HttpStatus._
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.{ SolrResponseBase, UpdateResponse }
import org.apache.solr.client.solrj.{ SolrClient, SolrQuery }
import org.apache.solr.common.{ SolrDocument, SolrInputDocument }

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

trait SolrImpl extends Solr with DebugEnhancedLogging {
  val solrClient: SolrClient

  override def search(itemId: String): Try[Option[SolrDocument]] = {
    val query = new SolrQuery {
      set("q", s"id=$itemId")
    }
    Try(solrClient.query(query))
      .flatMap(checkResponseStatus)
      .map(_.getResults.asScala.headOption)
      .recoverWith {
        case t: HttpSolrClient.RemoteSolrException if isParseException(t) =>
          Failure(SolrBadRequestException(t.getMessage, t))
        case t =>
          Failure(SolrSearchException(query.toQueryString, t))
      }
  }

  override def submit(solrFields: SolrLiterals): Try[UpdateResponse] = {
    Try(solrClient.add(new SolrInputDocument() {
      for ((k, v) <- solrFields) {
        addField(k, v)
      }
    }))
      .flatMap(checkResponseStatus)
      .recoverWith { case t => Failure(SolrUpdateException(solrFields, t)) }
  }

  override def delete(query: String): Try[UpdateResponse] = {
    val q = new SolrQuery {
      set("q", query)
    }
    Try(solrClient.deleteByQuery(q.getQuery))
      .flatMap(checkResponseStatus)
      .recoverWith {
        case t: HttpSolrClient.RemoteSolrException if isParseException(t) =>
          Failure(SolrBadRequestException(t.getMessage, t))
        case t =>
          Failure(SolrDeleteException(query, t))
      }
  }

  override def commit(): Try[UpdateResponse] = {
    Try(solrClient.commit())
      .flatMap(checkResponseStatus)
      .recoverWith { case t => Failure(SolrCommitException(t)) }
  }

  override def close(): Try[Unit] = {
    Try(solrClient.close())
  }

  private def isParseException(t: HttpSolrClient.RemoteSolrException) = {
    t.getRootThrowable.endsWith("ParseException")
  }

  private def checkResponseStatus[T <: SolrResponseBase](response: T): Try[T] = {
    // this method hides the inconsistent design of the solr library from the rest of the code
    Try(response.getStatus) match {
      case Success(0) | Success(SC_OK) => Success(response)
      case Success(_) => Failure(SolrStatusException(response.getResponse))
      case Failure(_: NullPointerException) => Success(response) // no status at all
      case Failure(t) => Failure(t)
    }
  }
}