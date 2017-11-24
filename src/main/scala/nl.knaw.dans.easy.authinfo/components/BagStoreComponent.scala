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

import java.net.{ URI, URLEncoder }
import java.nio.file.{ Path, Paths }
import java.util.UUID

import nl.knaw.dans.easy.authinfo.HttpStatusException

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }
import scalaj.http.Http

trait BagStoreComponent {

  val bagStore: BagStore

  def loadDDM(bagId: UUID): Try[Elem] = {
    bagStore.loadXML(bagId, Paths.get("metadata/dataset.xml"))
  }

  def loadFilesXML(bagId: UUID): Try[Elem] = {
    bagStore.loadXML(bagId, Paths.get("metadata/files.xml"))
  }

  def loadBagInfo(bagId: UUID): Try[String] = {
    for {
      url <- Try(URLEncoder.encode("bag-info.txt", "UTF8"))
      response = Http(url.toString).method("GET").asString
      _ <- if (response.isSuccess) Success(())
           else Failure(HttpStatusException(url.toString, response))
    } yield response.body
  }

  trait BagStore {
    val baseUri: URI

    def loadXML(bagId: UUID, path: Path): Try[Elem] = {
      for {
        f <- Try(URLEncoder.encode(path.toString, "UTF8"))
        url = baseUri.resolve(s"stores/pdbs/bags/$bagId/$f").toURL // TODO drop 'stores/pdbs' when easy-bag-store#43 not only merged but also versioned
        xml = XML.load(url)
      } yield xml
    }
  }
}
