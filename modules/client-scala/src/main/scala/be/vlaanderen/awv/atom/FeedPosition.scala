package be.vlaanderen.awv.atom

import java.net.URI

import be.vlaanderen.awv.atom.format.Url

/**
 * class be.vlaanderen.awv.atom.FeedPosition
 * @author Peter Rigole
 *         company <a href="http://www.Qmino.com">Qmino</a>
 *         Creation-Date: 9/05/14
 *         Creation-Time: 16:17
 */
case class FeedPosition(url: Url, index: Int, headers: Map[String, String] = Map.empty) {
  require(new URI(url.path).isAbsolute)
}