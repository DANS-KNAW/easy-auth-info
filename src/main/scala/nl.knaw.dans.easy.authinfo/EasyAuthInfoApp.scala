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

import nl.knaw.dans.easy.authinfo.Command.FeedBackMessage
import nl.knaw.dans.easy.authinfo.components.{ FileItem, FileRights }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node }

trait EasyAuthInfoApp extends AutoCloseable with DebugEnhancedLogging with ApplicationWiring {

  def delete(query: String): Try[FeedBackMessage] = {
    authCache
      .delete(query)
      .flatMap(_ => authCache.commit())
      .map(_ => s"Deleted documents for query $query ")
  }

  def rightsOf(bagId: UUID, path: Path): Try[Option[Result]] = {
    authCache.search(s"$bagId/$path") match {
      case Success(Some(doc)) => Success(Some(Result(FileItem.toJson(doc), None)))
      case Success(None) => fromBagStore(bagId, path)
      case Failure(t) =>
        logger.warn(s"cache lookup failed for [$bagId/$path] ${ Option(t.getMessage).getOrElse("") }")
        fromBagStore(bagId, path)
    }
  }

  private def fromBagStore(bagId: UUID, path: Path) = {
    itemFromFilesXML(bagId, path) match {
      case Failure(t) => Failure(t)
      case Success(None) => Success(None) // TODO cache repeatedly requested but not found bags/files?
      case Success(Some(filesXmlItem)) => collectInfo(bagId, path, filesXmlItem) match {
        case Failure(t) => Failure(t)
        case Success(fileItem) =>
          val cacheUpdate = Some(authCache.submit(fileItem.solrLiterals))
          Success(Some(Result(fileItem.json, cacheUpdate)))
      }
    }
  }

  private def itemFromFilesXML(bagId: UUID, path: Path) = {
    bagStore
      .loadFilesXML(bagId)
      .map(getFileNode(_, path))
  }

  private def collectInfo(bagId: UUID, path: Path, fileNode: Node) = {
    for {
      ddm <- bagStore.loadDDM(bagId)
      ddmProfile <- getTag(ddm, "profile", bagId)
      dateAvailable <- getTag(ddmProfile, "available", bagId).map(_.text)
      rights <- FileRights.get(ddmProfile, fileNode)
      bagInfo <- bagStore.loadBagInfo(bagId)
      owner <- getDepositor(bagInfo, bagId)
    } yield FileItem(bagId, path, owner, rights, dateAvailable)
  }

  private def getTag(node: Node, tag: String, bagId: UUID): Try[Node] = {
    Try { (node \ tag).head }
      .recoverWith { case _ => Failure(InvalidBagException(s"<ddm:$tag> not found in $bagId/dataset.xml")) }
  }

  private def getDepositor(bagInfoMap: BagInfo, bagId: UUID) = {
    Try(bagInfoMap("EASY-User-Account"))
      .recoverWith { case _ => Failure(InvalidBagException(s"'EASY-User-Account' (case sensitive) not found in $bagId/bag-info.txt")) }
  }

  def getFileNode(xmlDoc: Elem, path: Path): Option[Node] = {
    (xmlDoc \ "file").find(_
      .attribute("filepath")
      .map(_.text)
      .contains(path.toString)
    )
  }

  override def close(): Unit = {
    authCache.close()
  }
}

object EasyAuthInfoApp {
  def apply(conf: Configuration): EasyAuthInfoApp = new EasyAuthInfoApp {
    override lazy val configuration: Configuration = conf
  }
}
