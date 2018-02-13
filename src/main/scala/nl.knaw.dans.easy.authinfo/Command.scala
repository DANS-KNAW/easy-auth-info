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

import java.io.FileNotFoundException
import java.nio.file.{ Path, Paths }
import java.util.UUID

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.native.JsonMethods.{ pretty, render }
import resource._

import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val app = EasyAuthInfoApp(configuration)

  managed(app)
    .acquireAndGet(runSubCommand)
    .doIfSuccess(msg => println(msg))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${e.getClass.getName} ${ e.getMessage }") }

  private def runSubCommand(app: EasyAuthInfoApp): Try[FeedBackMessage] = {
    commandLine.subcommand match {
      case Some(commandLine.runService) => runAsService(app)
      case Some(fileCommand @ commandLine.file) => app.rightsOf(fileCommand.path())
      case _ => Failure(new IllegalArgumentException(s"Unknown command: ${ commandLine.subcommand }"))
    }
  }

  private def runAsService(app: EasyAuthInfoApp): Try[FeedBackMessage] = Try {
    val service = new EasyAuthInfoService(configuration.properties.getInt("daemon.http.port"), app)
    // TODO how to report cache problems?
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        service.stop()
        service.destroy()
      }
    })

    service.start()
    Thread.currentThread.join()
    "Service terminated normally."
  }
}
