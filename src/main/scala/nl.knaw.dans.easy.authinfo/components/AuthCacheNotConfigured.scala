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
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument

import scala.util.{ Failure, Success, Try }

trait AuthCacheNotConfigured {

  private val notImplemented = Failure(new NotImplementedError("no cache configured, assembling responses from bag store only"))

  def search(itemId: String): Try[Option[SolrDocument]] = Success(None)

  def submit(cacheFields: CacheLiterals): Try[UpdateResponse] = notImplemented

  def delete(query: String): Try[UpdateResponse] = notImplemented

  def commit(): Try[UpdateResponse] = notImplemented

  def close(): Try[Unit] = Success(())
}
object AuthCacheNotConfigured {
  type CacheLiterals = Seq[(String, String)]
}
