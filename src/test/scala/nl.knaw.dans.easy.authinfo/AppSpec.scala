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

import java.nio.file.Paths

import nl.knaw.dans.easy.authinfo.components.SolrMocker.expectsSolrDocInCache
import org.apache.solr.common.SolrDocument

import scala.util.{ Failure, Success }

class AppSpec extends TestSupportFixture {

  private val app = mockApp

  "rightsOf(path)" should "fail with /" in {
    inside(app.jsonRightsOf(Paths.get("/"))) {
      case Failure(e) => e should have message "can't extract valid UUID from [/]"
    }
  }

  it should "fail with just a uuid" in {
    inside(app.jsonRightsOf(Paths.get(s"$randomUUID"))) {
      case Failure(e) => e should have message s"can't extract bag relative path from [$randomUUID]"
    }
  }

  it should "fail with and empty path" in {
    inside(app.jsonRightsOf(Paths.get(s"$randomUUID/"))) {
      case Failure(e) => e should have message s"can't extract bag relative path from [$randomUUID]"
    }
  }

  it should "succeed with some.txt" in {
    expectsSolrDocInCache(new SolrDocument() {
      addField("id", s"$randomUUID/some%2Efile")
      // ignoring the other fields to avoid testing random order results
      // the full content is tested with ServletSpec (which is designed to manually test all the logging)
    })
    app.jsonRightsOf(Paths.get(s"$randomUUID/some.txt")) shouldBe Success(
      s"""{
         |  "itemId":"$randomUUID/some%2Efile"
         |}""".stripMargin
    )
  }
}
