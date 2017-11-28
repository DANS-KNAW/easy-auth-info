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

import java.nio.file.Path

import nl.knaw.dans.easy.authinfo.components.RightsFor._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node }

case class FileRights(accessibleTo: String, visibleTo: String)
class FileItems(ddmProfile: Node, filesXml: Elem) extends DebugEnhancedLogging {

  private val fileItems = filesXml \ "file"

  // see ddm.xsd EasyAccessCategoryType
  private lazy val datasetAccessibleTo = (ddmProfile \ "accessRights").text match {
    // @formatter:off
    case "OPEN_ACCESS"                      => Some(ANONYMOUS.toString)
    case "OPEN_ACCESS_FOR_REGISTERED_USERS" => Some(KNOWN.toString)
    case "GROUP_ACCESS"                     => Some(RESTRICTED_GROUP.toString)
    case "REQUEST_PERMISSION"               => Some(RESTRICTED_REQUEST.toString)
    case "NO_ACCESS"                        => Some(NONE.toString)
    case _                                  => None
    // @formatter:off
  }
  private val allowedValues = RightsFor.values.map(_.toString)

  def rightsOf(path: Path): Try[Option[FileRights]] = {
    fileItems
      .find(_
        .attribute("filepath")
        .map(_.text)
        .contains(path.toString)
      ).map(rightsAsJson) match {
      case Some(Success(v)) => Success(Some(v))
      case Some(Failure(t)) => Failure(t)
      case None => Success(None)
    }
  }

  private def rightsAsJson(item: Node): Try[FileRights] = {

    def getValue(tag: String): Option[String] = {
      (item \ tag).headOption.map(_.text)
    }
    val accessibleTo = getValue("accessibleToRights")
      .orElse(getValue("accessRights")
        .orElse(datasetAccessibleTo)
        .map(_.toUpperCase)
      )
    if(accessibleTo.isEmpty)
      Failure(new Exception("<accessibleToRights> not found in files.xml nor <ddm:accessRights> in dataset.xml"))
    else if (!allowedValues.contains(accessibleTo.getOrElse("?")))
             // <dcterms:accessRights> (synonym for <accessibleToRights>) content not validated by XSD
      Failure(new Exception(s"<dcterms:accessRights> [${accessibleTo.getOrElse("?")}] in files.xml should be one of: ${allowedValues.mkString(", ")}"))
    else {
      val visibleTo = getValue("visibleToRights").getOrElse(ANONYMOUS.toString)
      Success(FileRights(accessibleTo.getOrElse("?"), visibleTo))
    }
  }
}
