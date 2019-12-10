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

import nl.knaw.dans.easy.authinfo.components.RightsFor.ANONYMOUS
import nl.knaw.dans.easy.authinfo.components.FileRights
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration

import scala.util.Try
import scala.xml.Node

class Licenses(licenses: PropertiesConfiguration) extends DebugEnhancedLogging {

  private val CC_0_LICENSE = "http://creativecommons.org/publicdomain/zero/1.0"
  private val DANS_LICENSE = "http://dans.knaw.nl/en/about/organisation-and-policy/legal-information/DANSGeneralconditionsofuseUKDEF.pdf"

  def getLicense(dcmiMetadata: Node, rights: FileRights): Try[License] = Try {

    def getValue(tag: String): Option[String] = {
      if (dcmiMetadata.isEmpty)
        None
      else
        (dcmiMetadata \ tag).headOption.map(_.text)
    }

    val licenseKey = getValue("license")
    if (licenseKey.nonEmpty)
      License(licenseKey.get, getLicenseTitle(licenseKey.get))
    else {
      val key = if (rights.accessibleTo == ANONYMOUS.toString) CC_0_LICENSE else DANS_LICENSE
      License(key, getLicenseTitle(key))
    }
  }

  private def getLicenseTitle(licenseKey: String): String = {
    Option(licenses.getString(licenseKey)).getOrElse(licenseNotFound(licenseKey))
  }

  private def licenseNotFound(licenseKey: String): String = {
    logger.error("No license found with key: " + licenseKey)
    ""
  }
}