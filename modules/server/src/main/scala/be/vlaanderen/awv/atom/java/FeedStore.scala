package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{Context, Feed, FeedUpdateInfo, FeedInfo}

/**
 * Wrapper wround the [[be.vlaanderen.awv.atom.FeedStore]] that offers a Java-like interface.
 *
 * @tparam E type of the elements in the feed
 */
abstract class FeedStore[E] extends be.vlaanderen.awv.atom.FeedStore[E] {
  def underlying: be.vlaanderen.awv.atom.FeedStore[E]

  override def context: Context = underlying.context

  /**
   * Retrieves a page of the feed.
   *
   * @param page the page number
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(page: Long): Option[Feed[E]] = underlying.getFeed(page)

  /**
   * Updates the feed pages and feed info.
   *
   * @param feedUpdates
   * @param feedInfo
   */
  override def update(feedUpdates: List[FeedUpdateInfo[E]], feedInfo: FeedInfo): Unit = underlying.update(feedUpdates, feedInfo)

  /**
   * Gets the feed info.
   *
   * @return the feed info or None if the feed is not persisted yet
   */
  override def getFeedInfo: Option[FeedInfo] = underlying.getFeedInfo
}
