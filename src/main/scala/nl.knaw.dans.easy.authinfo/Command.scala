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

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource._

import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String
  println(args.toSeq)
  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val app = EasyAuthInfoApp(configuration)

  managed(app)
    .acquireAndGet(runCommand)
    .doIfSuccess(println)
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getClass.getName } ${ e.getMessage }") }

  private def runCommand(app: EasyAuthInfoApp): Try[FeedBackMessage] = {
    (commandLine.path.isDefined, commandLine.subcommand) match {
      case (false, Some(commandLine.runService)) => runAsService(app)
      case (true, None) => app.jsonRightsOf(commandLine.path())
      case (false, None) => Failure(new IllegalArgumentException(s"No command nor argument specified"))
      case _ => Failure(new IllegalArgumentException(s"Invalid command, options or arguments: " + commandLine.args.mkString(" ")))
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
