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

import java.net.URI

import nl.knaw.dans.easy.authinfo.components.{ AuthCacheNotConfigured, AuthCacheWithSolr, BagStoreComponent }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient

import scala.util.{ Success, Try }

/**
 * Initializes and wires together the components of this application.
 */
trait ApplicationWiring extends BagStoreComponent with DebugEnhancedLogging {

  /**
   * the application configuration
   */
  val configuration: Configuration

  override val bagStore: BagStore = new BagStore {
    override val baseUri: URI = new URI(configuration.properties.getString("bag-store.url"))
  }

  val authCache: AuthCacheNotConfigured = {
    // depending on isThrowExceptionOnMissing we get null or an exception
    (Try(Option(configuration.properties.getString("solr.url"))),
      Try(Option(configuration.properties.getString("solr.collection")))
    ) match {
      case (Success(Some(url)), Success(Some(collection))) =>
        val baseUrl = s"$url/$collection/"
        logger.info(s"Running with solr cache: $baseUrl")
        new AuthCacheWithSolr() {
          // TODO the solr core might still not be available, slowing down the service even more than having no solr.url configured
          override val solrClient: SolrClient = new HttpSolrClient.Builder(baseUrl).build()
          override val commitWithinMs: Int = configuration.properties.getInt("solr.commitWithinMs", 15000)
        }
      case _ =>
        logger.warn("RUNNING WITHOUT SOLR CACHE")
        new AuthCacheNotConfigured() {}
    }
  }
}
