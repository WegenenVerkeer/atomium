package be.wegenenverkeer.atomium.japi.server

import be.wegenenverkeer.atomium.format
import be.wegenenverkeer.atomium.server.Context

/**
 * Wrapper around the [[be.wegenenverkeer.atomium.server.FeedStore]] that offers a Java-like interface.
 *
 * @tparam E type of the elements in the feed
 */
abstract class FeedStore[E, C <: Context](feedName: String, title: Option[String]) {

  def underlying: be.wegenenverkeer.atomium.server.FeedStore[E, C]

  /**
   * Retrieves a page of the feed.
   *
   * @param startSequenceNr the starting entry's sequence number (exclusive), should not be returned in the feed page
   * @param pageSize the number of entries
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else ('backward') navigate to 'next' elements in feed (towards last page of feed)
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(startSequenceNr: Long, pageSize: Int, forward: Boolean, context: C): Option[format.Feed[E]] =
    underlying.getFeed(startSequenceNr, pageSize, forward)(context)

  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize the maximum number of feed entries to return. The page could contain less entries
   * @return the head of the feed
   */
  def getHeadOfFeed(pageSize: Int, context: C): Option[format.Feed[E]] =
    underlying.getHeadOfFeed(pageSize)(context)

  /**
   * Push entries onto the feed
   * @param entries the entries to push to the feed
   */
  def push(entries: Iterable[E], context: C) = underlying.push(entries)(context)

  def push(uuid: String, entry: E, context: C): Unit = underlying.push(uuid, entry)(context)

}
