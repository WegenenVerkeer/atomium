package be.wegenenverkeer.atom

import _root_.java.net.URI

/**
 * The feed position contains the data that indicates which entry was last consumed.
 *
 * @param url the url of the last retrieved page
 * @param index the index of the last item consumed on the last retrieved page
 */
case class FeedPosition(url: Url, index: Int, headers: Map[String, String] = Map.empty) {
  require(new URI(url.path).isAbsolute)
}
