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
package nl.knaw.dans.easy.authinfo.components

import java.nio.file.Paths

import nl.knaw.dans.easy.authinfo.{ License, TestSupportFixture }
import nl.knaw.dans.easy.authinfo.components.RightsFor._
import org.apache.solr.common.SolrDocument
import org.json4s.native.JsonMethods._

class FileItemSpec extends TestSupportFixture {

  "constructor" should "produce proper json" in {
    val rights = FileRights(KNOWN.toString, ANONYMOUS.toString)
    val license = License("http://opensource.org/licenses/MIT", "MIT.txt")
    val fileItem = FileItem(randomUUID, Paths.get("some/file.txt"), "someone", rights, "1992-07-30", license)
    pretty(render(fileItem.json)) shouldBe
      s"""{
         |  "itemId":"$randomUUID/some/file%2Etxt",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://opensource.org/licenses/MIT",
         |  "licenseTitle":"MIT.txt"
         |}""".stripMargin
  }

  "toJson" should "convert a SolrDocument without additional solr fields" in {
    val doc = new SolrDocument() {
      addField("id", s"$randomUUID/some/file%2Etxt")
      addField("easy_owner", "someone")
      addField("easy_date_available", "1992-07-30")
      addField("easy_accessible_to", "KNOWN")
      addField("easy_visible_to", "ANONYMOUS")
      addField("easy_license_key", "http://opensource.org/licenses/MIT")
      addField("easy_license_title", "MIT.txt")
      addField("solr_extras", "abcd")
    }
    val expected =
      s"""{
         |  "itemId":"$randomUUID/some/file%2Etxt",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS",
         |  "licenseKey":"http://opensource.org/licenses/MIT",
         |  "licenseTitle":"MIT.txt"
         |}""".stripMargin

    checkSameHashMaps(expected, FileItem.toJson(doc))
  }
}
