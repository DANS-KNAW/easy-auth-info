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

import java.net.{ URI, URL, URLEncoder }
import java.util.UUID

import nl.knaw.dans.easy.authinfo.HttpStatusException

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }
import scalaj.http.Http

trait BagStoreComponent {

  val bagStore: BagStore


  trait BagStore {
    val baseUri: URI

    def loadDDM(bagId: UUID): Try[Elem] = {
      toURL(bagId, "metadata/dataset.xml").map(XML.load)
    }

    def loadFilesXML(bagId: UUID): Try[Elem] = {
      toURL(bagId, "metadata/files.xml").map(XML.load)
    }

    def loadBagInfo(bagId: UUID): Try[String] = {
      loadAsString(bagId, "bag-info.txt")
    }

    private def loadAsString(bagId: UUID, path: String): Try[String] = {
      for {
        url <- toURL(bagId, path)
        response = Http(url.toString).method("GET").asString
        _ <- if (response.isSuccess) Success(())
             else Failure(HttpStatusException(url.toString, response))
      } yield response.body
    }

    private def toURL(bagId: UUID, path: String): Try[URL] = Try {
      val f = URLEncoder.encode(path, "UTF8")
      baseUri.resolve(s"stores/pdbs/bags/$bagId/$f").toURL // TODO drop 'stores/pdbs' when easy-bag-store#43 not only merged but also versioned
    }
  }
}
