package nl.knaw.dans.easy.authinfo.components

import nl.knaw.dans.easy.authinfo.components.AuthCache.CacheLiterals
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrDocument

import scala.util.{ Failure, Success, Try }

trait AuthCache {


  def search(itemId: String): Try[Option[SolrDocument]] =
    Success(None)

  def submit(cacheFields: CacheLiterals): Try[UpdateResponse] =
    Failure(new NotImplementedError())

  def delete(query: String): Try[UpdateResponse] =
    Failure(new NotImplementedError())

  def commit(): Try[UpdateResponse] =
    Failure(new NotImplementedError())

  def close(): Try[Unit] =
    Success(())
}
object AuthCache {
  type CacheLiterals = Seq[(String, String)]

}
