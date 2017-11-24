package nl.knaw.dans.easy.authinfo.components

import scala.util.Try

class BagInfo(content: String) {
  val properties: Try[Map[String, String]] = Try(content
    .split("\n")
    .map { line =>
      val parts = line.split(":")
      (parts.head.trim, parts.last.trim)
    }
    .toMap)
}
