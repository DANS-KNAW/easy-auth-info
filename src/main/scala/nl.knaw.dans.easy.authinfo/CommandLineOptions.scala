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

import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand, ValueConverter, singleArgConverter }

class CommandLineOptions(args: Array[String], configuration: Configuration) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))
  printedName = "easy-auth-info"
  version(configuration.version)
  private val SUBCOMMAND_SEPARATOR = "---\n"
  val description: String = s"""Provides consolidated authorization info about items in a bag store."""
  val synopsis: String =
    s"""
       |  $printedName [ run-service | <item-id> ]""".stripMargin

  version(s"$printedName v${ configuration.version }")
  banner(
    s"""
       |  $description
       |
       |Usage:
       |
       |$synopsis
       |
       |Options:
       |""".stripMargin)

  val path: ScallopOption[Path] = trailArg[Path](name = "path", required = false)

  //noinspection TypeAnnotation
  val runService = new Subcommand("run-service") {
    descr(
      "Starts EASY Auth Info as a daemon that services HTTP requests")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(runService)

  footer("")
}
