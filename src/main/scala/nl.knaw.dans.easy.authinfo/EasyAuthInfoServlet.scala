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

import java.nio.file.Paths
import java.util.UUID

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.native.JsonMethods.{ pretty, render }
import org.scalatra._

import scala.util.{ Failure, Success, Try }

class EasyAuthInfoServlet(app: EasyAuthInfoApp) extends ScalatraServlet with DebugEnhancedLogging {

  import app._

  get("/") {
    contentType = "text/plain"
    Ok("EASY Auth Info Service running...")
  }

  private def getUUID = {
    Try { UUID.fromString(params("uuid")) }
  }

  get("/:uuid/*") {
    contentType = "application/json"
    (getUUID, multiParams("splat")) match {
      case (Success(uuid), Seq(path)) => rightsOf(uuid, Paths.get(path)) match {
        case Success(Some(rights)) => Ok(pretty(render(rights)))
        case Success(None) => NotFound(s"$uuid/$path does not exist")
          // TODO bag store not available?
          // See https://github.com/DANS-KNAW/easy-update-solr4files-index/blob/055128d9dea0ea1013be178b7b6c795c0667a6e7/src/main/scala/nl.knaw.dans.easy.solr4files/SearchServlet.scala#L41-L43
        case Failure(t) =>
          logger.error(t.getMessage, t)
          InternalServerError("not expected exception")
      }
      case (Failure(t), _) => BadRequest(s"UUID missing or not valid")
      case _ => BadRequest("file path is missing")
    }
  }
}
