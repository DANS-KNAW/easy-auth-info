package nl.knaw.dans.easy.authinfo.components

import scala.util.Try

case class BagInfo(private val content: String) {
  val properties: Try[Map[String, String]] = Try(content
    .split("\n")
    .map { line =>
      val Array(k, v) = line.split(":", 2)
      (k.trim, v.trim)
    }
    .toMap)
}
