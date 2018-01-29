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

import java.nio.file.{ Path, Paths }
import java.util.UUID

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.eclipse.jetty.http.HttpStatus._
import org.json4s.native.JsonMethods.{ pretty, render }
import org.scalatra._

import scala.util.{ Failure, Success, Try }
import scalaj.http.HttpResponse

class EasyAuthInfoServlet(app: EasyAuthInfoApp) extends ScalatraServlet with DebugEnhancedLogging {

  get("/") {
    contentType = "text/plain"
    Ok("EASY Auth Info Service running...")
  }

  get("/:uuid/*") {
    contentType = "application/json"
    val result = (getUUID, getPath) match {
      case (Success(_), Success(None)) => BadRequest("file path is empty")
      case (Success(uuid), Success(Some(path))) => respond(uuid, path, app.rightsOf(uuid, path))
      case (Failure(t), _) => BadRequest(t.getMessage)
      case _ => InternalServerError("not expected exception")
    }
    logger.info(s"returned status=${ result.status } for request [${ params.mkString(", ") }] response.body: ${ result.body.toString.toOneLiner }")
    result
  }

  private def getUUID = {
    Try { UUID.fromString(params("uuid")) }
  }

  private def getPath = Try {
    multiParams("splat").find(_.trim.nonEmpty).map(Paths.get(_))
  }

  private def respond(uuid: UUID, path: Path, rights: Try[Option[Result]]) = {
    rights match {
      case Success(Some(Result(json, Some(Failure(t))))) =>
        logger.error(s"cache update failed for [$uuid/$path] reason: ${ t.getMessage.toOneLiner }")
        Ok(pretty(render(json)))
      case Success(Some(Result(json, Some(Success(_))))) =>
        logger.info(s"cache updated for [$uuid/$path]")
        Ok(pretty(render(json)))
      case Success(Some(Result(json, _))) =>
        Ok(pretty(render(json)))
      case Success(None) => NotFound(s"$uuid/$path does not exist")
      case Failure(HttpStatusException(message, HttpResponse(_, SERVICE_UNAVAILABLE_503, _))) => ServiceUnavailable(message)
      case Failure(HttpStatusException(message, HttpResponse(_, REQUEST_TIMEOUT_408, _))) => RequestTimeout(message)
      case Failure(HttpStatusException(message, HttpResponse(_, NOT_FOUND_404, _))) if message.startsWith("Bag ") => NotFound(s"$uuid does not exist")
      case Failure(HttpStatusException(message, HttpResponse(_, NOT_FOUND_404, _))) =>
        logger.error(s"invalid bag: $message")
        InternalServerError("not expected exception")
      case Failure(t: InvalidBagException) =>
        logger.error(s"invalid bag: ${ t.getMessage }")
        InternalServerError("not expected exception")
      case Failure(t) =>
        logger.error(t.getMessage, t)
        InternalServerError("not expected exception")
    }
  }
}
