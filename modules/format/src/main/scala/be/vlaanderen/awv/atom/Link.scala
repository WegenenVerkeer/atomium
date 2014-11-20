package be.vlaanderen.awv.atom

/**
 * Representation of a link in an Atom feed.
 *
 * @param rel
 * @param href
 */
case class Link(rel: String, href: Url)

object Link {

  val firstLink = "first"
  val lastLink = "last"
  val nextLink = "next"
  val previousLink = "previous"
  val selfLink = "self"
  val collectionLink = "collection"

}