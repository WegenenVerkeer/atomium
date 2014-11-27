package be.vlaanderen.awv.atom

/**
 * A feed store is responsible for the persistence of feeds.
 *
 * TODO: support to undo new additions (transactional)
 * 
 * @tparam E type of the elements in the feed
 */
abstract class FeedStore[E](feedName: String, title: Option[String], urlProvider: UrlBuilder) {

  def context: Context

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry
   * @param pageSize the number of entries
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(start:Int, pageSize: Int): Option[Feed[E]] = {
    for {
      entries <- Some(getFeedEntries(start, pageSize)); if entries.size > 0
    } yield Feed[E](
      id = (urlProvider.base / feedName).path,
      base = urlProvider.base,
      title = title,
      updated = entries.head.updated,
      links = List(Link(Link.selfLink, urlProvider.feedLink(start, pageSize)),
        Link(Link.lastLink, urlProvider.feedLink(0, pageSize))) ++
        getNextLink(start, pageSize) ++
        getPreviousLink(start, pageSize, maxId.toInt),
      entries = entries)
    }


  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize
   * @return the head of the feed
   */
  def getHeadOfFeed(pageSize: Int): Option[Feed[E]] = {
    val max: Int = maxId.toInt
    if (max > 0) {
      getFeed(((max-1) / pageSize) * pageSize, pageSize)
    } else {
      None
    }
  }

  /**
   * @return the maximum entry sequence number
   */
  def maxId: Long

  /**
   * return pageSize entries starting from start
   * @param start the start entry
   * @param pageSize the number of entries to return
   * @return
   */
  def getFeedEntries(start:Int, pageSize: Int): List[Entry[E]]

  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  def push(entries: Iterable[E])

  /**
   * This method is called when the [[be.vlaanderen.awv.atom.FeedService]] is started.
   * This can be used as a hook (to check consistency, for example)
   */
  def open()  : Unit = {}

  /**
   * This method is called when the [[be.vlaanderen.awv.atom.FeedService]] is stopped.
   */
  def close() : Unit = {}

}
