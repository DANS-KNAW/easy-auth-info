/*
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

import nl.knaw.dans.lib.encode.PathEncoding
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet._
import nl.knaw.dans.lib.string._
import org.eclipse.jetty.http.HttpStatus._
import org.json4s.native.JsonMethods.{ pretty, render }
import org.scalatra._
import scalaj.http.HttpResponse

import scala.util.{ Failure, Success, Try }

class EasyAuthInfoServlet(app: EasyAuthInfoApp) extends ScalatraServlet
  with ServletLogger
  with PlainLogFormatter
  with LogResponseBodyOnError
  with DebugEnhancedLogging {

  get("/") {
    contentType = "text/plain"
    Ok("EASY Auth Info Service running...")
  }

  get("/:uuid/*") {
    contentType = "application/json"
    (getUUID, getPath) match {
      case (Success(_), Success(None)) => BadRequest("file path is empty")
      case (Success(uuid), Success(Some(path))) => respond(uuid, path, app.authInfo(uuid, path))
      case (Failure(t), _) => BadRequest(t.getMessage)
      case _ => InternalServerError("not expected exception")
    }
  }

  private def getUUID: Try[UUID] = {
    params("uuid").toUUID.toTry
  }

  private def getPath = Try {
    // the find makes sure there is a path
    multiParams("splat").find(_.trim.nonEmpty).map(Paths.get(_))
  }

  private def respond(uuid: UUID, path: Path, authInfo: Try[Option[CachedAuthInfo]]): ActionResult = {
    authInfo match {
      case Success(Some(CachedAuthInfo(json, Some(Failure(t))))) =>
        logger.error(s"[$uuid] cache update failed for [$uuid/${path.escapePath}] reason: ${ t.getMessage.toOneLiner }")
        Ok(pretty(render(json)))
      case Success(Some(CachedAuthInfo(json, Some(Success(_))))) =>
        logger.info(s"[$uuid] cache updated for [$uuid/${path.escapePath}]")
        Ok(pretty(render(json)))
      case Success(Some(CachedAuthInfo(json, _))) =>
        Ok(pretty(render(json)))
      case Success(None) => NotFound(s"$uuid/${path.escapePath} does not exist")
      case Failure(HttpStatusException(message, HttpResponse(_, SERVICE_UNAVAILABLE_503, _))) => ServiceUnavailable(message)
      case Failure(ServiceNotAvailableException(_, e)) => ServiceUnavailable(e.getMessage)
      case Failure(BagDoesNotExistException(uuid: UUID)) => NotFound(s"$uuid/${path.escapePath} does not exist")
      case Failure(t: InvalidBagException) =>
        logger.error(s"[$uuid] invalid bag: ${ t.getMessage }")
        InternalServerError(s"bag $uuid appears to be invalid")
      case Failure(t) =>
        logger.error(s"[$uuid] unexpected error: ${t.getMessage}", t)
        InternalServerError("unexpected error")
    }
  }
}
