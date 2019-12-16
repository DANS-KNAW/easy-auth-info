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
import java.util.UUID

import nl.knaw.dans.easy.authinfo.License
import nl.knaw.dans.easy.authinfo.components.AuthCacheNotConfigured.CacheLiterals
import nl.knaw.dans.easy.authinfo.components.FileItem.solr2jsonKey
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.solr.common.SolrDocument
import org.json4s.JsonAST
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._

import scala.collection.JavaConverters._
import nl.knaw.dans.lib.encode._

case class FileItem(id: UUID, path: Path, owner: String, rights: FileRights, dateAvailable: String, license: License) extends DebugEnhancedLogging {

  val solrLiterals: CacheLiterals = Seq(
    ("id", s"$id/${path.escapePath}"),
    ("easy_owner", owner),
    ("easy_date_available", dateAvailable),
    ("easy_accessible_to", rights.accessibleTo.toString),
    ("easy_visible_to", rights.visibleTo.toString),
    ("easy_license_key", license.licenseKey),
    ("easy_license_title", license.licenseTitle),
  )
  val json: JObject = solrLiterals
    .map { case (key, value) => (solr2jsonKey(key), value) }
    .foldLeft(JObject())(_ ~ _)
}
object FileItem {
  private val solr2JsonKeys = Map[String, String](
    "id" -> "itemId",
    "easy_owner" -> "owner",
    "easy_visible_to" -> "visibleTo",
    "easy_accessible_to" -> "accessibleTo",
    "easy_date_available" -> "dateAvailable",
    "easy_license_key" -> "licenseKey",
    "easy_license_title" -> "licenseTitle",
  )

  private def solr2jsonKey(key: String): String = solr2JsonKeys.getOrElse(key, key)

  def toJson(solrDocument: SolrDocument): JsonAST.JObject = {
    val fieldValueMap = solrDocument.getFieldValueMap
    fieldValueMap
      .keySet()
      .asScala // asScala on the map throws UnsupportedOperationException
      .filter(_.matches("(id|easy_.*)")) // filter on the solr query would spread knowledge
      .map(key => (solr2jsonKey(key), fieldValueMap.get(key).toString))
      .foldLeft(JObject())(_ ~ _)
  }
}
