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

import java.util.UUID

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.eclipse.jetty.http.HttpStatus._
import org.scalatra._

import scala.util.Try
import scalaj.http.HttpResponse

class UpdateServlet(app: EasyAuthInfoApp) extends ScalatraServlet with DebugEnhancedLogging {

  get("/update") {
    contentType = "text/plain"
    Ok("EASY Auth Info update is running.")
  }

  private def respond(result: Try[String]): ActionResult = {
    val msgPrefix = "Log files should show which actions succeeded. Finally failed with: "
    result.map(Ok(_))
      .doIfFailure { case e => logger.error(e.getMessage, e) }
      .getOrRecover {
        case CacheBadRequestException(message, _) => BadRequest(message)
        case HttpStatusException(message, HttpResponse(_, NOT_FOUND_404, _)) => NotFound(message)
        case HttpStatusException(message, HttpResponse(_, SERVICE_UNAVAILABLE_503, _)) => ServiceUnavailable(message)
        case HttpStatusException(message, HttpResponse(_, REQUEST_TIMEOUT_408, _)) => RequestTimeout(message)
        case t =>
          logger.error(s"not expected exception", t)
          InternalServerError(t.getMessage) // for an internal servlet we can and should expose the cause
      }
  }

  private def getUUID = {
    Try { UUID.fromString(params("uuid")) }
  }

  private def badUuid(e: Throwable) = {
    BadRequest(e.getMessage)
  }

  delete("/:uuid") {
    val result = getUUID
      .map(uuid => respond(app.delete(s"id:$uuid/*")))
      .getOrRecover(badUuid)
    logger.info(s"delete returned ${ result.status } (${ result.body }) for $params")
    result
  }

  delete("/store/:store") {
    NotImplemented // TODO get uuid's from bag store
  }

  delete("/all") {
    val result = respond(app.delete("*"))
    logger.info(s"delete returned ${ result.status } (${ result.body }) for $params")
    result
  }
}
