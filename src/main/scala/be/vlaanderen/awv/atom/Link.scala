package be.vlaanderen.awv.atom

case class Link(rel: String, href: Url)

object Link {

  val firstLink = "first"
  val nextLink = "next"
  val previousLink = "previous"
  val selfLink = "self"
  val collectionLink = "collection"

}