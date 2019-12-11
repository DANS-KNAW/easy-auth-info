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

import nl.knaw.dans.easy.authinfo.{ License, TestSupportFixture }

import scala.util.Success
import scala.xml.{ Elem, Node }

class LicensesSpec extends TestSupportFixture {

  private val app = mockApp

  private val dcmiMetadataNoLicense: Node =
    <ddm:dcmiMetadata>
        <dcterms:hasVersion>1.1</dcterms:hasVersion>
        <dcterms:modified>2015-09-08</dcterms:modified>
        <dcterms:rightsHolder>I Lastname</dcterms:rightsHolder>
    </ddm:dcmiMetadata>

  private val dcmiMetadataWithLicense: Elem =
    <ddm:dcmiMetadata>
        <dcterms:hasVersion>1.1</dcterms:hasVersion>
        <dcterms:modified>2015-09-08</dcterms:modified>
        <dcterms:license xsi:type="dcterms:URI">http://opensource.org/licenses/MIT</dcterms:license>
        <dcterms:rightsHolder>I Lastname</dcterms:rightsHolder>
    </ddm:dcmiMetadata>

  "getLicense" should "return CC0 license when there is no dcmiMetadata element and dataset is Open Access" in {
    app.configuration.licenses.getLicense(None, FileRights("ANONYMOUS", "ANONYMOUS")) shouldBe
      Success(License("http://creativecommons.org/publicdomain/zero/1.0", "CC0-1.0.html"))
  }

  it should "return DANS license when there is no dcmiMetadata element and dataset is not Open Access" in {
    app.configuration.licenses.getLicense(None, FileRights("KNOWN", "KNOWN")) shouldBe
      Success(License("http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSGeneralconditionsofuseUKDEF.pdf", "DANS_Licence_UK.pdf"))
  }

  it should "return CC0 license when there is no license in dcmiMetadata element and dataset is Open Access" in {
    app.configuration.licenses.getLicense(Option(dcmiMetadataNoLicense), FileRights("ANONYMOUS", "ANONYMOUS")) shouldBe
      Success(License("http://creativecommons.org/publicdomain/zero/1.0", "CC0-1.0.html"))
  }

  it should "return DANS license when there is no license in dcmiMetadata element and dataset is not Open Access" in {
    app.configuration.licenses.getLicense(Option(dcmiMetadataNoLicense), FileRights("KNOWN", "KNOWN")) shouldBe
      Success(License("http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSGeneralconditionsofuseUKDEF.pdf", "DANS_Licence_UK.pdf"))
  }

  it should "return the license in dcmiMetadata element when it is given in dataset.xml" in {
    app.configuration.licenses.getLicense(Option(dcmiMetadataWithLicense), FileRights("KNOWN", "KNOWN")) shouldBe
      Success(License("http://opensource.org/licenses/MIT", "MIT.txt"))
  }
}
