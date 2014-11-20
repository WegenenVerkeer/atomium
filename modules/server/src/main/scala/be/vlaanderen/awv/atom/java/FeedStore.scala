package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{Feed, UrlBuilder, Context}

/**
 * Wrapper wround the [[be.vlaanderen.awv.atom.FeedStore]] that offers a Java-like interface.
 *
 * @tparam E type of the elements in the feed
 */
abstract class FeedStore[E] extends be.vlaanderen.awv.atom.FeedStore[E] {
  def underlying: be.vlaanderen.awv.atom.FeedStore[E]

  override def context: Context = underlying.context

  override def urlProvider: UrlBuilder = underlying.urlProvider

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry
   * @param pageSize the number of entries in the feed page            
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(start: Int, pageSize: Int): Option[Feed[E]] = underlying.getFeed(start, pageSize)

  /**
   * retrieve the head of the feed, i.e. the feed page containing the most recent entries
   * @param pageSize the number of entries to return
   * @return the head of the feed
   */
  override def getHeadOfFeed(pageSize: Int): Option[Feed[E]] = underlying.getHeadOfFeed(pageSize)

  /**
   * Push entries onto the feed
   * @param entries the entries to push to the feed
   */
  override def push(entries: Iterable[E]) = underlying.push(entries)

}
