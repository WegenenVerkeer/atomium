package be.wegenenverkeer.atom

import be.wegenenverkeer.atomium.format.Feed

/**
 * A feed store is responsible for the persistence of feeds.
 *
 * @tparam E type of the elements in the feed
 */
trait FeedStore[E] {

  /**
   * Retrieves a page of the feed.
   *
   * @param startSequenceNr the starting entry's sequence number (exclusive), should not be returned in the feed page
   * @param count the number of entries to return in the feed page, can be less if page is incomplete
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else ('backward') navigate to 'next' elements in feed (towards last page of feed)
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(startSequenceNr: Long, count: Int, forward: Boolean): Option[Feed[E]]

  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize the maximum number of feed entries to return. The page could contain less entries
   * @return the head of the feed
   */
  def getHeadOfFeed(pageSize: Int): Option[Feed[E]]

  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  def push(entries: Iterable[E]) : Unit

  /**
   * push a single entry to the feed
   * @param entry the entry to push to the feed
   */
  def push(entry: E): Unit = {
    push(List(entry))
  }

  def push(uuid: String, entry: E): Unit

  /**
   * This method is called when the [[be.wegenenverkeer.atom.FeedService]] is started.
   * This can be used as a hook (to check consistency, for example)
   */
  def open(): Unit = {}

  /**
   * This method is called when the [[be.wegenenverkeer.atom.FeedService]] is stopped.
   */
  def close(): Unit = {}

}
