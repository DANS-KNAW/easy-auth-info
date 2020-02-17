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

import java.net.{ ConnectException, URI, URL }
import java.nio.file.Paths
import java.util.UUID

import nl.knaw.dans.lib.encode.PathEncoding
import nl.knaw.dans.easy.authinfo.{ BagDoesNotExistException, BagInfo, HttpStatusException, ServiceNotAvailableException }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import scalaj.http.HttpResponse

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }

trait BagStoreComponent extends DebugEnhancedLogging {
  this: HttpContext =>

  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI
    val connTimeout: Int
    val readTimeout: Int

    private val serviceName = "easy-bag-store"
    private val bagNotFoundMessageRegex = ".*Bag [0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12} does not exist in BagStore.*"

    def loadDDM(bagId: UUID): Try[Elem] = {
      logger.info(s"[$bagId] retrieving ddm.xml")
      toURL(bagId, "metadata/dataset.xml").flatMap(loadXml)
    }

    def loadFilesXML(bagId: UUID): Try[Elem] = {
      logger.info(s"[$bagId] retrieving files.xml")
      toURL(bagId, "metadata/files.xml").flatMap(loadXml)
        .recoverWith {
          case HttpStatusException(msg: String, response: HttpResponse[String]) if isBagDoesNotExistErrorMessage(msg, response.body) => Failure(BagDoesNotExistException(bagId))
        }
    }

    def loadBagInfo(bagId: UUID): Try[BagInfo] = {
      logger.info(s"[$bagId] retrieving bag-info.txt")
      toURL(bagId, "bag-info.txt").flatMap(loadBagInfo)
    }

    private def loadXml(url: URL): Try[Elem] = {
      for {
        response <- Try { Http(url.toString).timeout(connTimeout, readTimeout).asString }
        _ <- if (response.isSuccess) Success(())
             else Failure(HttpStatusException(url.toString, response))
      } yield XML.loadString(response.body)
    }.recoverWith {
      case e: ConnectException => Failure(ServiceNotAvailableException(serviceName, e))
    }

    private def loadBagInfo(url: URL): Try[BagInfo] = {
      for {
        response <- Try { Http(url.toString).timeout(connTimeout, readTimeout).asString }
        _ <- if (response.isSuccess) Success(())
             else Failure(HttpStatusException(url.toString, response))
      } yield response
        .body
        .split("\n")
        .map { line =>
          val Array(k, v) = line.split(":", 2)
          (k.trim, v.trim)
        }.toMap
    }.recoverWith {
      case e: ConnectException => Failure(ServiceNotAvailableException(serviceName, e))
    }

    private def toURL(bagId: UUID, path: String): Try[URL] = Try {
      val escapedPath = Paths.get(path).escapePath
      baseUri.resolve(s"bags/$bagId/$escapedPath").toURL
    }

    private def isBagDoesNotExistErrorMessage(message: String, body: String): Boolean = message.startsWith("Bag ") || body.matches(bagNotFoundMessageRegex)
  }
}
