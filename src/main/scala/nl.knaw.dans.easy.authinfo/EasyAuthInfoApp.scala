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

import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.easy.authinfo.components.FileItems
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._

import scala.util.{ Failure, Success, Try }
import scala.xml.Node

trait EasyAuthInfoApp extends AutoCloseable with DebugEnhancedLogging with ApplicationWiring {

  def rightsOf(bagId: UUID, path: Path): Try[Option[JValue]] = {
    for {
      filesXml <- bagStore.loadFilesXML(bagId)
      // TODO skip the rest if path not in files.xml, see find in FileItems.rightsOf
      ddm <- bagStore.loadDDM(bagId)
      ddmProfile <- getTag(ddm, "profile")
      dateAvailable <- getTag(ddmProfile, "available").map(_.text)
      rights <- new FileItems(ddmProfile, filesXml).rightsOf(path)
      bagInfo <- bagStore.loadBagInfo(bagId)
      owner <- getDepositor(bagInfo)
    } yield rights.map(value =>
      ("itemId" -> s"$bagId/$path") ~
        ("owner" -> owner) ~
        ("dateAvailable" -> dateAvailable) ~
        ("accessibleTo" -> value.accessibleTo) ~
        ("visibleTo" -> value.visibleTo)
    )
  }

  private def getTag(node: Node, tag: String): Try[Node] = {
    Try { (node \ tag).head }
      .recoverWith { case t => Failure(new Exception(s"<ddm:$tag> not found in dataset.xml [${ t.getMessage }]")) }
  }

  private def getDepositor(bagInfoMap: BagInfo) = {
    Try(bagInfoMap("EASY-User-Account"))
      .recoverWith { case t => Failure(new Exception(s"'EASY-User-Account' (case sensitive) not found in bag-info.txt [${ t.getMessage }]")) }
  }

  def init(): Try[Unit] = {
    // Do any initialization of the application here. Typical examples are opening
    // databases or connecting to other services.
    Success(())
  }

  override def close(): Unit = {

  }
}

object EasyAuthInfoApp {
  def apply(conf: Configuration): EasyAuthInfoApp = new EasyAuthInfoApp {
    override lazy val configuration: Configuration = conf
  }
}
