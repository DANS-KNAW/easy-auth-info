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
package nl.knaw.dans.easy

import nl.knaw.dans.easy.authinfo.components.AuthCacheNotConfigured.CacheLiterals
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.util.NamedList
import org.json4s.JsonAST.JValue

import scala.util.Try
import scalaj.http.HttpResponse

package object authinfo {

  type BagInfo = Map[String, String]

  /**
   * @param authInfo    authorisation information
   * @param cacheUpdate None: not updated because it was found
   */
  case class CachedAuthInfo(authInfo: JValue, cacheUpdate: Option[Try[UpdateResponse]] = None)

  implicit class RichString(val s: String) extends AnyVal {

    // TODO candidate for dans-scala-lib
    def toOneLiner: String = s.split("\n").map(_.trim).mkString(" ")
  }

  case class HttpStatusException(msg: String, response: HttpResponse[String])
    extends Exception(s"$msg - ${ response.statusLine }, details: ${ response.body }")

  case class CacheStatusException(namedList: NamedList[AnyRef])
    extends Exception(s"solr returned: ${ namedList.asShallowMap().values().toArray().mkString }")

  case class CacheBadRequestException(msg: String, cause: Throwable)
    extends Exception(msg, cause)

  case class CacheSearchException(query: String, cause: Throwable)
    extends Exception(s"solr query [$query] failed with ${ cause.getMessage }", cause)

  case class CacheUpdateException(literals: CacheLiterals, cause: Throwable)
    extends Exception(s"solr update of [${ literals.toMap.mkString(", ") }] failed with ${ cause.getMessage }", cause)

  case class CacheCommitException(cause: Throwable)
    extends Exception(cause.getMessage, cause)

  case class InvalidBagException(message: String) extends Exception(message)
}
