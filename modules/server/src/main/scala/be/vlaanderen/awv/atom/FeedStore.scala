package be.vlaanderen.awv.atom

/**
 * A feed store is responsible for the persistence of feeds.
 *
 * TODO: support to undo new additions (transactional)
 * 
 * @tparam E type of the elements in the feed
 */
trait FeedStore[E] {

  def context: Context

  /**
   * Retrieves a page of the feed.
   *
   * @param page the page number
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(page: Long): Option[Feed[E]]

  /**
   * Gets the feed info.
   * 
   * @return the feed info or None if the feed is not persisted yet
   */
  def getFeedInfo: Option[FeedInfo]

  /**
   * Updates the feed pages and feed info.
   * 
   * @param feedUpdates
   * @param feedInfo
   */
  def update(feedUpdates: List[FeedUpdateInfo[E]], feedInfo: FeedInfo)

  /**
   * This method is called when the [[be.vlaanderen.awv.atom.FeedPusher]] is started.
   *
   * This can be used as a hook (to check consistency, for example)
   */
  def open = {}

  /**
   * This method is called when the [[be.vlaanderen.awv.atom.FeedPusher]] is stopped.
   */
  def close = {}
}
