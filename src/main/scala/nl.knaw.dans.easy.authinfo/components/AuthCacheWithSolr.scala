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

import nl.knaw.dans.easy.authinfo.components.AuthCacheNotConfigured.CacheLiterals
import nl.knaw.dans.easy.authinfo.{ CacheBadRequestException, CacheSearchException, CacheStatusException, CacheUpdateException }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.http.HttpStatus._
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.{ SolrResponseBase, UpdateResponse }
import org.apache.solr.client.solrj.{ SolrClient, SolrQuery }
import org.apache.solr.common.{ SolrDocument, SolrInputDocument }

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

trait AuthCacheWithSolr extends AuthCacheNotConfigured with DebugEnhancedLogging {
  val solrClient: SolrClient
  val commitWithinMs: Int

  override def search(itemId: String): Try[Option[SolrDocument]] = {
    val query = new SolrQuery {
      set("q", s"id:$itemId")
    }
    Try(solrClient.query(query))
      .flatMap(checkResponseStatus)
      .map(_.getResults.asScala.headOption)
      .recoverWith {
        case t: HttpSolrClient.RemoteSolrException if isParseException(t) =>
          Failure(CacheBadRequestException(t.getMessage, t))
        case t =>
          Failure(CacheSearchException(query.toQueryString, t))
      }
  }

  override def submit(solrFields: CacheLiterals): Try[UpdateResponse] = {
    val document = new SolrInputDocument() {
      for ((k, v) <- solrFields) {
        addField(k, v)
      }
    }
    Try(solrClient.add(document, commitWithinMs))
      .flatMap(checkResponseStatus)
      .recoverWith { case t => Failure(CacheUpdateException(solrFields, t)) }
  }


  override def close(): Unit = {
    logger.info(s"closing SOLR cache")
    solrClient.commit() // don't care about a failure, loosing a few pending changes is a minor performance loss
    solrClient.close()
  }

  private def isParseException(t: HttpSolrClient.RemoteSolrException) = {
    t.getRootThrowable.endsWith("ParseException")
  }

  private def checkResponseStatus[T <: SolrResponseBase](response: T): Try[T] = {
    // this method hides the inconsistent design of the solr library from the rest of the code
    Try(response.getStatus) match {
      case Success(0) | Success(SC_OK) => Success(response)
      case Success(_) => Failure(CacheStatusException(response.getResponse))
      case Failure(_: NullPointerException) => Success(response) // no status at all
      case Failure(t) => Failure(t)
    }
  }
}