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

import org.rogach.scallop.{ ScallopConf, ScallopOption, Subcommand }

class CommandLineOptions(args: Array[String], configuration: Configuration) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))
  printedName = "easy-auth-info"
  version(configuration.version)
  private val SUBCOMMAND_SEPARATOR = "---\n"
  val description: String = s"""Provides consolidated authorization info about items in a bag store."""
  val synopsis: String =
    s"""
       |  $printedName get <item-id> # Retrieves the information from the cache or directly from the bag-store
       |  $printedName delete <solr-query> # Delete the selected item(s) from the SOLR index
       |  $printedName run-service # Runs the program as a service
       |
       |  Some examples of standard solr queries for the delete command:
       |
       |    everything:                        '*'
       |    all files of a particular user:    'easy_owner:<name>'
       |    all files of a particular dataset: 'id:<bag-id>/*'
       |""".stripMargin

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

  val get = new Subcommand("get") {
    descr("Retrieve the authorization info about the given item in a bagstore")
    val itemId: ScallopOption[Path] = trailArg[Path](name = "itemId")
    footer(SUBCOMMAND_SEPARATOR)
  }
  val delete = new Subcommand("delete") {
    descr("Delete documents from the SOLR index")
    val query: ScallopOption[String] = trailArg[String](name = "solr-query")
    footer(SUBCOMMAND_SEPARATOR)
  }
  val runService = new Subcommand("run-service") {
    descr("Starts EASY Auth Info as a daemon that services HTTP requests")
    footer(SUBCOMMAND_SEPARATOR)
  }
  addSubcommand(get)
  addSubcommand(delete)
  addSubcommand(runService)

  footer("")
  verify()
}
