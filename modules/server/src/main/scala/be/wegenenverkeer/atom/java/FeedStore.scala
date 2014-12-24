package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.UrlBuilder

/**
 * Wrapper around the [[be.wegenenverkeer.atom.FeedStore]] that offers a Java-like interface.
 *
 * @tparam E type of the elements in the feed
 */
abstract class FeedStore[E](feedName: String, title: Option[String], urlProvider: UrlBuilder)
  extends be.wegenenverkeer.atom.FeedStore[E] {

  def underlying: be.wegenenverkeer.atom.FeedStore[E]

  /**
   * Retrieves a page of the feed.
   *
   * @param startSequenceNr the starting entry's sequence number (exclusive), should not be returned in the feed page
   * @param pageSize the number of entries
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else ('backward') navigate to 'next' elements in feed (towards last page of feed)
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(startSequenceNr:Long, pageSize: Int, forward: Boolean): Option[be.wegenenverkeer.atom.Feed[E]] = underlying.getFeed(startSequenceNr, pageSize, forward)

  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize the maximum number of feed entries to return. The page could contain less entries
   * @return the head of the feed
   */
  override def getHeadOfFeed(pageSize: Int): Option[be.wegenenverkeer.atom.Feed[E]] = underlying.getHeadOfFeed(pageSize)

  /**
   * Push entries onto the feed
   * @param entries the entries to push to the feed
   */
  override def push(entries: Iterable[E]) = underlying.push(entries)

  override def push(uuid: String, entry: E): Unit = underlying.push(uuid, entry)

}
