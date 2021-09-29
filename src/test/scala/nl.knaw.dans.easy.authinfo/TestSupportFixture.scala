/*
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

import java.nio.file.{ Files, Path, Paths }
import java.util.UUID

import nl.knaw.dans.easy.authinfo.components.SolrMocker.mockedSolrClient
import nl.knaw.dans.easy.authinfo.components.{ AuthCacheNotConfigured, AuthCacheWithSolr }
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.apache.solr.client.solrj.SolrClient
import org.json4s
import org.json4s.native.JsonMethods.parse
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterEach, Inside }

import scala.collection.immutable.HashMap

trait TestSupportFixture extends AnyFlatSpec with Matchers with Inside with BeforeAndAfterEach with MockFactory {

  lazy val testDir: Path = {
    val path = Paths.get(s"target/test/${ getClass.getSimpleName }").toAbsolutePath
    FileUtils.deleteQuietly(path.toFile)
    Files.createDirectories(path)
    path
  }

  val randomUUID: UUID = UUID.randomUUID()
  val uuidCentaur: UUID = UUID.fromString("9da0541a-d2c8-432e-8129-979a9830b427")
  val uuidAnonymized: UUID = UUID.fromString("1afcc4e9-2130-46cc-8faf-2663e199b218")

  /**
   * @param expectedJsonString a map with the expected key-value pairs
   * @param actual             a map with the actual key-value pairs in any order
   */
  def checkSameHashMaps(expectedJsonString: String, actual: json4s.JValue): Unit = {
    val expectedMap = parse(expectedJsonString).values.asInstanceOf[HashMap.HashTrieMap[String, String]]
    val actualMap = actual.values.asInstanceOf[HashMap.HashTrieMap[String, String]]

    // because of the random order we have to check the elements one by one
    actualMap.keySet shouldBe expectedMap.keySet
    for (key <- expectedMap.keySet)
      actualMap(key.toString) shouldBe expectedMap(key)
  }

  def mockApp: EasyAuthInfoApp = {
    new EasyAuthInfoApp {
      // mocking at a low level to test the chain of error handling
      override val bagStore: BagStore = mock[BagStore]
      override lazy val configuration: Configuration = new Configuration("", new PropertiesConfiguration() {
        addProperty("bag-store.url", "http://hostThatDoesNotExist:20110/")
        addProperty("bag-store.connection-timeout-ms", "1")
        addProperty("bag-store.read-timeout-ms", "1")
        addProperty("solr.url", "http://hostThatDoesNotExist")
        addProperty("solr.collection", "authinfo")
      },
        licenses = new Licenses(new PropertiesConfiguration() {
          addProperty("http://creativecommons.org/publicdomain/zero/1.0", "CC0-1.0.html")
          addProperty("http://opensource.org/licenses/MIT", "MIT.txt")
          addProperty("http://creativecommons.org/licenses/by/4.0", "CC-BY-4.0.html")
          addProperty("http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSLicence.pdf=DANS_Licence_UK.pdf", "DANS_Licence_UK.pdf")
        }
        ))
      override val authCache: AuthCacheNotConfigured = new AuthCacheWithSolr() {
        override val commitWithinMs = 1
        override val solrClient: SolrClient = mockedSolrClient
      }
    }
  }
}
