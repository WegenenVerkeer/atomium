package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.{FeedContent, Link, Feed}

/**
 * A feed store is responsible for the persistence of feeds.
 *
 * TODO: support to undo new additions (transactional)
 * 
 * @tparam E type of the elements in the feed
 */
trait FeedStore[E <: FeedContent] {

  def context: Context
  def urlProvider: UrlBuilder

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry
   * @param pageSize the number of entries
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(start: Int, pageSize: Int): Option[Feed[E]]

  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize
   * @return the head of the feed
   */
  def getHeadOfFeed(pageSize: Int): Option[Feed[E]]

  protected final def getHeadOfFeed(pageSize:Int, max: Int): Option[Feed[E]] = {
      if (max > 0) {
        getFeed(((max-1) / pageSize) * pageSize, pageSize)
      } else {
        None
      }
  }
  
  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  def push(entries: Iterable[E])

  /**
   * push a single entry to the feed
   * @param entry the entry to push to the feed
   */
  def push(entry: E): Unit  = {
    push(List(entry))
  }

  /**
   * This method is called when the [[be.vlaanderen.awv.atom.FeedService]] is started.
   * This can be used as a hook (to check consistency, for example)
   */
  def open()  : Unit = {}

  /**
   * This method is called when the [[be.vlaanderen.awv.atom.FeedService]] is stopped.
   */
  def close() : Unit = {}

  protected def getNextLink(start: Int, count: Int) : Option[Link] = {
    if (start - count >= 0)
      Some(link(Link.nextLink, start-count, count))
    else
      None
  }

  protected def getPreviousLink(start: Int, count: Int, total: Int): Option[Link] = {
    if (start + count < total)
      Some(link(Link.previousLink, start+count, count))
    else
      None
  }

  protected def link(l: String, start: Int, pageSize: Int): Link = {
    Link(l, urlProvider.feedLink(start, pageSize))
  }

}
