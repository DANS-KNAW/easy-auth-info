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

import nl.knaw.dans.easy.authinfo.TestSupportFixture
import nl.knaw.dans.easy.authinfo.components.RightsFor._

import scala.util.{ Failure, Success }
import scala.xml.Elem

class FileRightsSpec extends TestSupportFixture {

  private val openAccessProfile: Elem =
      <ddm:profile>
        <ddm:accessRights>OPEN_ACCESS</ddm:accessRights>
        <ddm:available>1992-07-30</ddm:available>
      </ddm:profile>

  it should "use the dataset rights" in {
    FileRights.get(openAccessProfile, <file filepath="some.file"></file>) shouldBe
      Success(FileRights(accessibleTo = "ANONYMOUS", visibleTo = "ANONYMOUS"))
  }

  it should "report an invalid DDM" in {
    inside(FileRights.get(<ddm:profile/>, <file filepath="some.file"></file>)) {
      case Failure(t) => t.getMessage shouldBe
        "<accessibleToRights> not found in files.xml nor <ddm:accessRights> in dataset.xml"
    }
  }

  it should "use a mix of file rights and dataset rights" in {
    FileRights.get(
      openAccessProfile,
      <file filepath="some.file">
        <accessibleToRights>{RESTRICTED_GROUP}</accessibleToRights>
      </file>
    ) shouldBe
      Success(FileRights(accessibleTo = "RESTRICTED_GROUP", visibleTo = "ANONYMOUS"))
  }

  it should "use file rights" in {
    FileRights.get(
      <ddm:profile/>,
      <file filepath="some.file">
        <accessibleToRights>{NONE}</accessibleToRights>
        <visibleToRights>{RESTRICTED_REQUEST}</visibleToRights>
      </file>
    ) shouldBe
      Success(FileRights(accessibleTo = NONE.toString, visibleTo = RESTRICTED_REQUEST.toString))
  }

  it should "ignore <dcterms:accessRights> if there is an <accessibleToRights>" in {
    FileRights.get(
      <ddm:profile/>,
      <file filepath="some.file">
        <dcterms:accessRights>{KNOWN}</dcterms:accessRights>
        <accessibleToRights>{NONE}</accessibleToRights>
        <visibleToRights>{RESTRICTED_REQUEST}</visibleToRights>
      </file>
    ) shouldBe
      Success(FileRights(accessibleTo = NONE.toString, visibleTo = RESTRICTED_REQUEST.toString))
  }

  it should "use <dcterms:accessRights> if there is no <accessibleToRights>" in {
    FileRights.get(
      <ddm:profile/>,
      <file filepath="some.file">
        <dcterms:accessRights>{KNOWN}</dcterms:accessRights>
        <visibleToRights>{RESTRICTED_REQUEST}</visibleToRights>
      </file>
    ) shouldBe
      Success(FileRights(accessibleTo = "KNOWN", visibleTo = "RESTRICTED_REQUEST"))
  }

  it should "report invalid <dcterms:accessRights>" in {
    inside(FileRights.get(
      <ddm:profile/>,
      <file filepath="some.file">
        <dcterms:accessRights>rubbish</dcterms:accessRights>
        <visibleToRights>{KNOWN}</visibleToRights>
      </file>
    )) {
      case Failure(t) => t.getMessage shouldBe
        "<dcterms:accessRights> [RUBBISH] in files.xml should be one of: ANONYMOUS, KNOWN, NONE, RESTRICTED_GROUP, RESTRICTED_REQUEST"
    }
  }
}
