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

import java.nio.file.Paths

import nl.knaw.dans.easy.authinfo.TestSupportFixture
import org.json4s.JsonDSL._

import scala.util.{ Failure, Success }
import scala.xml.Elem

class FileItemSpec extends TestSupportFixture {

  private val openAccessDDM: Elem = <ddm:DDM><ddm:profile><ddm:accessRights>OPEN_ACCESS</ddm:accessRights></ddm:profile></ddm:DDM>
  private val emptyDDM: Elem = <ddm:DDM></ddm:DDM>

  "rightsOf" should "return none" in {
    new FileItems(
      emptyDDM,
      <files></files>
    ).rightsOf(Paths.get("some.file")) shouldBe Success(None)
  }

  it should "use the dataset rights" in {
    new FileItems(
      openAccessDDM,
      <files><file filepath="some.file"></file></files>
    ).rightsOf(Paths.get("some.file")) shouldBe
      Success(Some(("accessibleTo" -> "ANONYMOUS") ~ ("visibleTo" -> "ANONYMOUS")))
  }

  it should "report an invalid DDM" in {
    inside(new FileItems(
      emptyDDM,
      <files><file filepath="some.file"></file></files>
    ).rightsOf(Paths.get("some.file"))
    ) {
      case Failure(t) => t.getMessage shouldBe "missing or invalid dataset access rights"
    }
  }

  it should "use a mix of file rights and dataset rights" in {
    new FileItems(
      openAccessDDM,
      <files><file filepath="some.file">
        <accessibleToRights>RESTRICTED_GROUP</accessibleToRights>
      </file></files>
    ).rightsOf(Paths.get("some.file")) shouldBe
      Success(Some(("accessibleTo" -> "RESTRICTED_GROUP") ~ ("visibleTo" -> "ANONYMOUS")))
  }

  it should "use file rights" in {
    new FileItems(
      openAccessDDM,
      <files><file filepath="some.file">
        <accessibleToRights>NONE</accessibleToRights>
        <visibleToRights>RESTRICTED_REQUEST</visibleToRights>
      </file></files>
    ).rightsOf(Paths.get("some.file")) shouldBe
      Success(Some(("accessibleTo" -> "NONE") ~ ("visibleTo" -> "RESTRICTED_REQUEST")))
  }
}
