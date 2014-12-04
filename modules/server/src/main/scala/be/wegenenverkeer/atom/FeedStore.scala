package be.wegenenverkeer.atom

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
   * @param start the starting entry
   * @param pageSize the number of entries
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(start:Int, pageSize: Int): Option[Feed[E]]


  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize
   * @return the head of the feed
   */
  def getHeadOfFeed(pageSize: Int): Option[Feed[E]]

  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  def push(entries: Iterable[E])

  /**
   * push a single entry to the feed
   * @param entry the entry to push to the feed
   */
  def push(entry: E): Unit = {
    push(List(entry))
  }

  /**
   * This method is called when the [[be.wegenenverkeer.atom.FeedService]] is started.
   * This can be used as a hook (to check consistency, for example)
   */
  def open()  : Unit = {}

  /**
   * This method is called when the [[be.wegenenverkeer.atom.FeedService]] is stopped.
   */
  def close() : Unit = {}

}
