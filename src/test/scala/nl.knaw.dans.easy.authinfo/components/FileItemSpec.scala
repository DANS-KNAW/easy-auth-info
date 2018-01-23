package nl.knaw.dans.easy.authinfo.components

import java.nio.file.Paths

import nl.knaw.dans.easy.authinfo.TestSupportFixture
import nl.knaw.dans.easy.authinfo.components.RightsFor._
import org.apache.solr.common.SolrDocument
import org.json4s.native.JsonMethods._

class FileItemSpec extends TestSupportFixture {

  "constructor" should "produce proper json" in {
    val rights = FileRights(KNOWN.toString, ANONYMOUS.toString)
    val fileItem = FileItem(randomUUID, Paths.get("some/file.txt"), "someone", rights, "1992-07-30")
    pretty(render(fileItem.json)) shouldBe
      s"""{
         |  "itemId":"$randomUUID/some/file.txt",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin
  }

  "toJson" should "convert a SolrDocument without additional solr fields" in {
    val doc = new SolrDocument() {
      addField("id", s"$randomUUID/some/file.txt")
      addField("easy_owner", "someone")
      addField("easy_date_available", "1992-07-30")
      addField("easy_accessible_to", "KNOWN")
      addField("easy_visible_to", "ANONYMOUS")
      addField("solr_extras", "abcd")
    }
    val expected =
      s"""{
         |  "itemId":"$randomUUID/some/file.txt",
         |  "owner":"someone",
         |  "dateAvailable":"1992-07-30",
         |  "accessibleTo":"KNOWN",
         |  "visibleTo":"ANONYMOUS"
         |}""".stripMargin

    checkSameHashMaps(expected, FileItem.toJson(doc))
  }
}
