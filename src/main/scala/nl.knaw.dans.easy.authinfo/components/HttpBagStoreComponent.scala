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
import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.easy.authinfo.HttpStatusException

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }
import scalaj.http.Http

trait HttpBagStoreComponent extends BagStoreComponent {

  trait HttpBagStore extends BagStore {
    val baseUri: URI

    override def loadXML(bagId: UUID, path: Path): Try[Elem] = {
      for {
        f <- Try(URLEncoder.encode(path.toString, "UTF8"))
        url = baseUri.resolve(s"stores/pdbs/bags/$bagId/$f").toURL // TODO drop 'stores/pdbs' when easy-bag-store#43 not only merged but also versioned
        content <- getContent(url)
        xml = XML.loadString(content)
      } yield xml
    }

    private def getContent(url: URL) = {
      Try(Http(url.toString).method("GET").asString).flatMap {
        case response if response.isSuccess => Success(response.body)
        case response => Failure(HttpStatusException(s"getContent($url)", response))
      }
    }
  }
}
