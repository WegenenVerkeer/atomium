package be.vlaanderen.awv.atom

/**
 * Representation of a link in an Atom feed.
 *
 * @param rel the kind of relation
 * @param href link url
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