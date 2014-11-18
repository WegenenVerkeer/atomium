package be.vlaanderen.awv.atom.format

import java.net.URI
import org.joda.time.DateTime

case class Feed[T <: FeedContent](base: Url, id:String, title: Option[String] = None,
                   generator: Option[Generator] = None, updated: DateTime,
                   links: List[Link],
                   entries: List[Entry[T]],
                   headers: Map[String, String] = Map.empty) {


  assert(links.exists(_.rel == Link.selfLink), "Link to self is mandatory")

  val selfLink : Link = findLinkByName(Link.selfLink).get // safe, since invariant is checked in constructor
  val nextLink : Option[Link] = findLinkByName(Link.nextLink)
  val firstLink : Option[Link] = findLinkByName(Link.firstLink)
  val previousLink : Option[Link] = findLinkByName(Link.previousLink)
  val lastLink : Option[Link] = findLinkByName(Link.lastLink)
  val collectionLink : Option[Link] = findLinkByName(Link.collectionLink)

  def findLinkByName(linkName:String) : Option[Link] = {
    links.collectFirst {
      case link @ Link(`linkName`, _) => link
    }
  }

  val baseUri = new URI(base.path)
  require(baseUri.isAbsolute)

  def resolveUrl(url : Url) = {
    new Url(baseUri.resolve(url.path).toString)
  }

  def calcETag(acceptHeader: String) = {
    val m = java.security.MessageDigest.getInstance("MD5")
    m.update(acceptHeader.getBytes("UTF-8"))
    m.update(baseUri.toString.getBytes("UTF-8"))
    m.update(id.getBytes("UTF-8"))
//    m.update(updated.toString.getBytes("UTF-8"))
    links foreach { link =>
      m.update(link.toString().getBytes("UTF-8"))
    }
    entries foreach { entry =>
      m.update(entry.content.value.toString().getBytes("UTF-8"))
    }
    new java.math.BigInteger(1, m.digest()).toString(16)
  }

  /**
   * @return true if this Feed page is complete, i.e. no more entries will ever be added to it.
   *         This can be used to set appropriate HTTP caching headers
   */
  def complete() = {
    links.count(_.rel == Link.previousLink) == 1
  }

}