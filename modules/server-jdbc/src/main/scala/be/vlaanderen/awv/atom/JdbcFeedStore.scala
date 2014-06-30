package be.vlaanderen.awv.atom

class JdbcFeedStore[E] extends FeedStore[E] {

  /**
   * Retrieves a page of the feed.
   *
   * @param page the page number
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(page: Long): Option[Feed[E]] = ???

  /**
   * Updates the feed pages and feed info.
   *
   * @param feedUpdates
   * @param feedInfo
   */
  override def update(feedUpdates: List[FeedUpdateInfo[E]], feedInfo: FeedInfo): Unit = ???

  /**
   * Gets the feed info.
   *
   * @return the feed info or None if the feed is not persisted yet
   */
  override def getFeedInfo: Option[FeedInfo] = ???
}
