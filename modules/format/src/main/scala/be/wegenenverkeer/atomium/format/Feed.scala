package be.wegenenverkeer.atomium.format

import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest

import org.joda.time.DateTime

/**
 * Representation of a (page in an) Atom feed.
 *
 * @param id the feed ID
 * @param base the base URI
 * @param title the feed title
 * @param updated indicates when the feed was last updated
 * @param generator the feed generator
 * @param links the links associated with this feed
 * @param entries the entries in the feed page
 * @tparam T the type of entry
 */
case class Feed[+T](id: String,
                   base: Url,
                   title: Option[String],
                   generator: Option[Generator] = None,
                   updated: DateTime,
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

  require(baseUri.isAbsolute, "base Url must be absolute")
  require(!base.path.endsWith("/"), "base Url must NOT end with a trailing /")

  def resolveUrl(url : Url) = {
    val resolvedUrl = new URI(baseUri.toString + "/" + url.path)
    new Url(resolvedUrl.normalize().toString)
  }

  def calcETag: String = {

    val message = MessageDigest.getInstance("MD5")
    val utf8 = "UTF-8"

    message.update(baseUri.toString.getBytes(utf8))
    message.update(id.getBytes(utf8))

    links foreach { link =>
      message.update(link.toString.getBytes(utf8))
    }

    entries foreach { entry =>
      message.update(entry.content.value.toString.getBytes(utf8))
      entry.links foreach { link =>
        message.update(link.toString.getBytes(utf8))
      }
    }

    new BigInteger(1, message.digest()).toString(16)
  }

  /**
   * @return true if this Feed page is complete, i.e. no more entries will ever be added to it.
   *         This can be used to set appropriate HTTP caching headers
   */
  def complete() = {
    links.exists(_.rel == Link.previousLink)
  }

}
