package be.wegenenverkeer.atom

import _root_.java.net.URI

/**
 * The feed position contains the data that indicates which entry was last consumed.
 *
 * @param url the url of the last retrieved page
 * @param entryId the entry id of the last consumed item
 */
case class FeedPosition(url: Url, entryId: Option[String]) {
  require(new URI(url.path).isAbsolute)
}
