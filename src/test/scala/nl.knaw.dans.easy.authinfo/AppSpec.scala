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

import java.lang.Throwable
import java.nio.file.Paths
import java.util.UUID

import org.apache.commons.configuration.PropertiesConfiguration
import org.json4s.JsonDSL._
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }
import scala.xml.Elem

class AppSpec extends TestSupportFixture
  with ScalatraSuite
  with MockFactory {

  private class Wiring extends ApplicationWiring(new Configuration("", new PropertiesConfiguration() {
    addProperty("bag-store.url", "http://localhost:20110/")
  }))
  private val wiring = mock[Wiring]
  private val app = new EasyAuthInfoApp(wiring)
  private val uuid = UUID.randomUUID()
  private val ddmOpenAccess: Elem = <ddm:DDM><ddm:profile><ddm:accessRights>OPEN_ACCESS</ddm:accessRights></ddm:profile></ddm:DDM>

  "rightsOf" should "return none" in {
    wiring.loadDDM _ expects uuid once() returning Success(ddmOpenAccess)
    wiring.loadFilesXML _ expects uuid once() returning Success(<files></files>)
    app.rightsOf(uuid, Paths.get("pakbon.xml")) shouldBe Success(None)
  }

  it should "use the dataset rights" in {
    wiring.loadDDM _ expects uuid once() returning Success(ddmOpenAccess)
    wiring.loadFilesXML _ expects uuid once() returning Success(
      <files><file filepath="pakbon.xml"></file></files>
    )
    inside(app.rightsOf(uuid, Paths.get("pakbon.xml"))) {
      case Success(Some(json)) => json shouldBe
        ("accessibleTo" -> "ANONYMOUS") ~
        ("visibleTo" -> "ANONYMOUS")
    }
  }

  it should "use a mix of file rights and dataset rights" in {
    wiring.loadDDM _ expects uuid once() returning Success(ddmOpenAccess)
    wiring.loadFilesXML _ expects uuid once() returning Success(
      <files><file filepath="pakbon.xml"><accessibleToRights>RESTRICTED_GROUP</accessibleToRights></file></files>
    )
    inside(app.rightsOf(uuid, Paths.get("pakbon.xml"))) {
      case Success(Some(json)) => json shouldBe
        ("accessibleTo" -> "RESTRICTED_GROUP") ~
        ("visibleTo" -> "ANONYMOUS")
    }
  }

  it should "use file rights" in {
    wiring.loadDDM _ expects uuid once() returning Success(ddmOpenAccess)
    wiring.loadFilesXML _ expects uuid once() returning Success(
      <files><file filepath="pakbon.xml">
        <accessibleToRights>NONE</accessibleToRights>
        <visibleToRights>RESTRICTED_REQUEST</visibleToRights>
      </file></files>
    )
    inside(app.rightsOf(uuid, Paths.get("pakbon.xml"))) {
      case Success(Some(json)) => json shouldBe
        ("accessibleTo" -> "NONE") ~
          ("visibleTo" -> "RESTRICTED_REQUEST")
    }
  }

  it should "report an invalid DDM" in {
    wiring.loadDDM _ expects uuid once() returning Success(<ddm:DDM></ddm:DDM>)
    wiring.loadFilesXML _ expects uuid once() returning Success(
      <files><file filepath="pakbon.xml"></file></files>
    )
    inside(app.rightsOf(uuid, Paths.get("pakbon.xml"))) {
      case Failure(t) => t should have message "missing or invalid dataset accessrights"
    }
  }
}