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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node }

class FileItems(ddm: => Elem, filesXml: Elem) extends DebugEnhancedLogging {

  private val fileItems = filesXml \ "file"

  // see ddm.xsd EasyAccessCategoryType
  private lazy val datasetAccessibleTo = (ddm \ "profile" \ "accessRights").text match {
    // @formatter:off
    case "OPEN_ACCESS"                      => Some("ANONYMOUS")
    case "OPEN_ACCESS_FOR_REGISTERED_USERS" => Some("KNOWN")
    case "GROUP_ACCESS"                     => Some("RESTRICTED_GROUP")
    case "REQUEST_PERMISSION"               => Some("RESTRICTED_REQUEST")
    case "NO_ACCESS"                        => Some("NONE")
    case _                                  => None
    // @formatter:off
  }

  def rightsOf(path: Path): Try[Option[JValue]] = {
    fileItems
      .find(_
        .attribute("filepath")
        .map(_.text)
        .contains(path.toString)
      ).map(rigthsAsJson) match {
      case Some(Success(v)) => Success(Some(v))
      case Some(Failure(t)) => Failure(t)
      case None => Success(None)
    }
  }

  private def rigthsAsJson(item: Node): Try[JValue] = {
    val a = getRigths(item, "accessibleToRights")
    val v = getRigths(item, "visibleToRights")
    if(a.isEmpty || v.isEmpty) Failure(new Exception("missing or invalid dataset access rights"))
    else Success(("accessibleTo" -> a.getOrElse("?")) ~ ("visibleTo" -> v.getOrElse("?")))
  }

  private def getRigths(item: Node, accessRights: String): Option[String] = {
    (item \ accessRights).map(_.text).mkString match {
      case "" => datasetAccessibleTo
      case s => Some(s)
    }
  }
}
