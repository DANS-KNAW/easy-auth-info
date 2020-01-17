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

import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.easy.authinfo.Command.FeedBackMessage
import nl.knaw.dans.easy.authinfo.components.{ FileItem, FileRights }
import nl.knaw.dans.lib.encode.PathEncoding
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.string._
import org.json4s.native.JsonMethods.{ pretty, render }

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node }

trait EasyAuthInfoApp extends AutoCloseable with DebugEnhancedLogging with ApplicationWiring {

  def authInfo(bagId: UUID, bagRelativePath: Path): Try[Option[CachedAuthInfo]] = {
    logger.info(s"[$bagId] retrieving authorization info of file $bagRelativePath")
    authCache.search(s"$bagId/${ bagRelativePath.escapePath }") match {
      case Success(Some(doc)) =>
        logger.info(s"[$bagId] Obtained authorization info for file $bagRelativePath from the authCache")
        Success(Some(CachedAuthInfo(FileItem.toJson(doc))))
      case Success(None) => fromBagStore(bagId, bagRelativePath)
      case Failure(t) =>
        logger.warn(s"[$bagId] cache lookup failed for file $bagRelativePath ${ Option(t.getMessage).getOrElse("") }")
        fromBagStore(bagId, bagRelativePath)
    }
  }

  /** @param fullPath <UUID>/<bag-relative-path> */
  def jsonAuthInfo(fullPath: Path): Try[String] = authInfo(fullPath).flatMap {
    case Some(CachedAuthInfo(authInfo, Some(Success(_)) | None)) => Success(pretty(render(authInfo)))
    case Some(CachedAuthInfo(_, Some(Failure(e)))) => Failure(e)
    case None => Failure(new FileNotFoundException(fullPath.toString))
  }

  def delete(query: String): Try[FeedBackMessage] = {
    logger.info(s"deleting documents with query '$query'")
    for {
      _ <- authCache.delete(query)
      _ <- authCache.commit()
    } yield s"Deleted documents with query '$query'"
  } recoverWith {
    case t: CacheCommitException => Failure(t)
    case t =>
      authCache.commit()
      Failure(t)
  }

  /** @param fullPath <UUID>/<bag-relative-path> */
  private def authInfo(fullPath: Path): Try[Option[CachedAuthInfo]] = for {
    uuid <- extractUUID(fullPath)
    subPath <- extractBagRelativePath(fullPath)
    cachedAuthInfo <- authInfo(uuid, subPath)
  } yield cachedAuthInfo

  /** @param fullPath <UUID>/<bag-relative-path> */
  private def extractBagRelativePath(fullPath: Path) = {
    Try(fullPath.subpath(1, fullPath.getNameCount))
      .recoverWith { case t => Failure(new Exception(s"can't extract bag relative path from [$fullPath]", t)) }
  }

  /** @param fullPath <UUID>/<bag-relative-path> */
  private def extractUUID(fullPath: Path): Try[UUID] = {
    Try { fullPath.getName(0).toString } match {
      case Success(name) => name.toUUID.toTry
      case t => Failure(new Exception(s"can't extract valid UUID from [$fullPath]"))
    }
  }

  private def fromBagStore(bagId: UUID, path: Path): Try[Option[CachedAuthInfo]] = {
    logger.info(s"[$bagId] auth-info for file $path not found in authCache, trying to retrieve it from bagStore")
    bagStore
      .loadFilesXML(bagId)
      .map(getFileNode(_, path))
      .flatMap {
        case None => Success(None) // TODO cache repeatedly requested but not found bags/files?
        case Some(filesXmlItem) =>
          collectInfo(bagId, path, filesXmlItem).map { fileItem =>
            val cacheUpdate = for {
              cacheUpdate <- authCache.submit(fileItem.solrLiterals)
              _ <- authCache.commit()
            } yield cacheUpdate
            Some(CachedAuthInfo(fileItem.json, Some(cacheUpdate)))
          }
      }
  }

  private def collectInfo(bagId: UUID, path: Path, fileNode: Node): Try[FileItem] = {
    for {
      ddm <- bagStore.loadDDM(bagId)
      ddmProfile <- getTag(ddm, "profile", bagId)
      dcmiMetadata = (ddm \ "dcmiMetadata").headOption
      dateAvailable <- getTag(ddmProfile, "available", bagId).map(_.text)
      rights <- FileRights.get(ddmProfile, fileNode)
      license <- configuration.licenses.getLicense(dcmiMetadata, rights)
      bagInfo <- bagStore.loadBagInfo(bagId)
      owner <- getDepositor(bagInfo, bagId)
    } yield FileItem(bagId, path, owner, rights, dateAvailable, license)
  }

  private def getTag(node: Node, tag: String, bagId: UUID): Try[Node] = {
    Try { (node \ tag).head }
      .recoverWith { case _ => Failure(InvalidBagException(s"<ddm:$tag> not found in $bagId/dataset.xml")) }
  }

  private def getDepositor(bagInfoMap: BagInfo, bagId: UUID): Try[String] = {
    Try(bagInfoMap("EASY-User-Account"))
      .recoverWith { case _ => Failure(InvalidBagException(s"'EASY-User-Account' (case sensitive) not found in $bagId/bag-info.txt")) }
  }

  private def getFileNode(xmlDoc: Elem, path: Path): Option[Node] = {
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
