package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.{Context, UrlBuilder}

/**
 * Wrapper around the [[be.wegenenverkeer.atom.FeedStore]] that offers a Java-like interface.
 *
 * @tparam E type of the elements in the feed
 */
abstract class FeedStore[E](feedName: String, title: Option[String], urlProvider: UrlBuilder)
  extends be.wegenenverkeer.atom.AbstractFeedStore[E](feedName, title, urlProvider) {

  def underlying: be.wegenenverkeer.atom.AbstractFeedStore[E]

  override def context: Context = underlying.context

  /**
   * return pageSize entries starting from start
   * @param start the start entry
   * @param pageSize the number of entries to return
   * @return
   */
  override def getFeedEntries(start:Long, pageSize: Int): List[be.wegenenverkeer.atom.Entry[E]] = underlying.getFeedEntries(start, pageSize)

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry
   * @param pageSize the number of entries in the feed page            
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(start: Long, pageSize: Int): Option[be.wegenenverkeer.atom.Feed[E]] = underlying.getFeed(start, pageSize)

  /**
   * retrieve the head of the feed, i.e. the feed page containing the most recent entries
   * @param pageSize the number of entries to return
   * @return the head of the feed
   */
  override def getHeadOfFeed(pageSize: Int): Option[be.wegenenverkeer.atom.Feed[E]] = underlying.getHeadOfFeed(pageSize)

  /**
   * Push entries onto the feed
   * @param entries the entries to push to the feed
   */
  override def push(entries: Iterable[E]) = underlying.push(entries)

  override def maxId: Long = underlying.maxId

}
