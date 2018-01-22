package nl.knaw.dans.easy.authinfo.components

import nl.knaw.dans.easy.authinfo.components.Solr.SolrLiterals
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument

import scala.util.{ Failure, Success, Try }

trait Solr {


  def search(itemId: String): Try[Option[SolrDocument]] =
    Success(None)

  def submit(solrFields: SolrLiterals): Try[UpdateResponse] =
    Failure(new NotImplementedError())

  def delete(query: String): Try[UpdateResponse] =
    Failure(new NotImplementedError())

  def commit(): Try[UpdateResponse] =
    Failure(new NotImplementedError())
}
object Solr {
  type SolrLiterals = Seq[(String, String)]

}
