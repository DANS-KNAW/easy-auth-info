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

import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.easy.authinfo.components.FileItems
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.JsonAST.{ JString, JValue }

import scala.util.{ Failure, Success, Try }

class EasyAuthInfoApp(wiring: ApplicationWiring) extends AutoCloseable with DebugEnhancedLogging {


  def rightsOf(bagId: UUID, path: Path): Try[Option[JValue]] = {
    for {
      ddm <- wiring.loadDDM(bagId) // TODO read lazily
      filesXml <- wiring.loadFilesXML(bagId)
      rights <- FileItems(ddm, filesXml).rightsOf(path)
    } yield rights
  }

  def init(): Try[Unit] = {
    // Do any initialization of the application here. Typical examples are opening
    // databases or connecting to other services.
    Success(())
  }

  override def close(): Unit = {

  }
}