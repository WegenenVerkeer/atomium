package be.wegenenverkeer.atom

import org.joda.time.LocalDateTime

/**
 * A feed store is responsible for the persistence of feeds.
 *
 * @tparam E type of the elements in the feed
 */
abstract class AbstractFeedStore[E](feedName: String, title: Option[String], urlProvider: UrlBuilder) {

  def context: Context

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry
   * @param pageSize the number of entries
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(start:Long, pageSize: Int): Option[Feed[E]] = {
    for {
      entries <- Some(getFeedEntries(start, pageSize)); if entries.size > 0 || start < maxId
    } yield Feed[E](
      id = (urlProvider.base / feedName).path,
      base = urlProvider.base,
      title = title,
      updated = entries match {
        case Nil => new LocalDateTime(0)
        case h :: t => h.updated
      },
      links = List(Link(Link.selfLink, urlProvider.feedLink(start, pageSize)),
        Link(Link.lastLink, urlProvider.feedLink(1, pageSize))) ++
        getNextLink(start, pageSize) ++
        getPreviousLink(start, pageSize, maxId.toInt),
      entries = entries)
    }


  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @return the head of the feed
   */
  def getHeadOfFeed(pageSize: Int): Option[Feed[E]] = {
    val max: Int = maxId.toInt
    if (max > 0) {
      getFeed(((max - 1) / pageSize) * pageSize + 1, pageSize)
    } else {
      None
    }
  }

  /**
   * @return the maximum entry sequence number
   */
  def maxId: Long

  /**
   * returns a maximum of pageSize entries starting from start sequence number.
   * You must make sure that you never return entries with a sequence number > start+pageSize,
   * because there might be some sequence numbers missing (f.e. in case of rolled back
   * transactions) and if we would not enforce this there might possibly be pages containing the same entries
   * @param start the sequence number of the starting entry
   * @param pageSize the maximum number of entries to return
   * @return
   */
  def getFeedEntries(start:Long, pageSize: Int): List[Entry[E]]

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

  protected def getNextLink(start: Long, count: Int) : Option[Link] = {
    if (start - count >= 0)
      Some(link(Link.nextLink, start - count, count))
    else
      None
  }

  protected def getPreviousLink(start: Long, count: Int, max: Int): Option[Link] = {
    if (start + count < max)
      Some(link(Link.previousLink, start + count, count))
    else
      None
  }

  protected def link(l: String, start: Long, pageSize: Int): Link = {
    Link(l, urlProvider.feedLink(start, pageSize))
  }

}
