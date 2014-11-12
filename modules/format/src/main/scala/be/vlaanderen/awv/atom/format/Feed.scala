package be.vlaanderen.awv.atom.format

import java.net.URI

case class Feed[T](base: Url,
                   title: Option[String], 
                   updated: String, 
                   links: List[Link], 
                   entries: List[Entry[T]],
                   headers: Map[String, String] = Map.empty) {

  assert(links.exists(_.rel == Link.selfLink), "Link to self is mandatory")
  require(new URI(base.path).isAbsolute)

  val selfLink : Link = findLinkByName(Link.selfLink).get // safe, since invariant is checked in constructor
  val nextLink : Option[Link] = findLinkByName(Link.nextLink)
  val firstLink : Option[Link] = findLinkByName(Link.firstLink)
  val previousLink : Option[Link] = findLinkByName(Link.previousLink)
  val lastLink : Option[Link] = findLinkByName(Link.lastLink)
  val collectionLink : Option[Link] = findLinkByName(Link.collectionLink)

  lazy val baseUri = new URI(base.path)

  def findLinkByName(linkName:String) : Option[Link] = {
    links.collectFirst {
      case link @ Link(`linkName`, _) => link
    }
  }

  def resolveUrl(url : Url) = {
    new Url(baseUri.resolve(url.path).toString)
  }

  def calcETag() = {
    val m = java.security.MessageDigest.getInstance("MD5")
    m.update(updated.getBytes("UTF-8"))
    entries foreach { entry =>
      m.update(entry.content.value.toString().getBytes("UTF-8"))
    }
    new java.math.BigInteger(1, m.digest()).toString(16)
  }

  def complete() = {
    links.count(_.rel == Link.previousLink) == 1
  }

}