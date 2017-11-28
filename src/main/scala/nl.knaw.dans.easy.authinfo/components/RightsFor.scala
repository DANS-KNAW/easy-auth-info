package nl.knaw.dans.easy.authinfo.components

object RightsFor extends Enumeration {
  type Margin = Value
  val NONE, KNOWN, ANONYMOUS, RESTRICTED_GROUP, RESTRICTED_REQUEST = Value
}
