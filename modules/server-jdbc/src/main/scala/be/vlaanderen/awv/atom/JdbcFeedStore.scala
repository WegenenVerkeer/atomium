package be.vlaanderen.awv.atom

class JdbcFeedStore[E](c: JdbcContext) extends FeedStore[E] {

  lazy val context = c

  /**
   * Retrieves a page of the feed.
   *
   * @param page the page number
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(page: Long): Option[Feed[E]] = {
    implicit val session = c.session
    None
  }

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
