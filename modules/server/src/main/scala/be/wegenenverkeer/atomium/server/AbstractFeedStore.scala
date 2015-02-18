package be.wegenenverkeer.atomium.server

import java.util.UUID

import be.wegenenverkeer.atomium.format.{Entry, Feed, Link, Url}

/**
 * A feed store is responsible for the persistence of feeds.
 * This abstract class serves as a base class for more specific FeedStore implementations.
 *
 * @tparam E type of the elements in the feed
 */
abstract class AbstractFeedStore[E, C <: Context](feedName: String,
                                                  title: Option[String],
                                                  url: Url) extends FeedStore[E, C] {

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry (exclusive), should not be returned in the feed page
   * @param pageSize the number of entries
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else navigate to 'next' elements in feed (towards last page of feed)
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(start: Long, pageSize: Int, forward: Boolean)(implicit context: C): Option[Feed[E]] = {
    require(pageSize > 0)
    val allowed =
      if (forward)
        start < maxId && getNumberOfEntriesLowerThan(start) % pageSize == 0
      else
        start <= maxId && getNumberOfEntriesLowerThan(start, inclusive = false) % pageSize == 0

    if (allowed) {
      // retrieve two entries more then requested and start is inclusive in next call
      // this is done to determine if there is a next and/or previous entry relative to the requested page
      val entries = getFeedEntries(start, pageSize + 2, forward)
      if (entries.size > 0) {
        val result = if (forward)
          processForwardEntries(start, pageSize, entries)
        else
          processBackwardEntries(start, pageSize, entries)
        toFeed(pageSize, result.feedEntries, result.previousSequenceNr, result.nextSequenceNr)
      } else
        None
    } else {
      None
    }
  }

  private[this] def processForwardEntries(start: Long,
                                          pageSize: Int,
                                          entries: List[FeedEntry]): ProcessedFeedEntries = {
    require(entries != Nil)

    var nextId: Option[Long] = None
    var previousId: Option[Long] = None
    var feedEntries: List[FeedEntry] = Nil

    //entries are sorted by id ascending
    if (start == entries.head.sequenceNr) {
      //start should be excluded
      feedEntries = entries.tail.take(pageSize).reverse
      nextId = Some(start) //nextId is start
    } else {
      //there is no next page
      feedEntries = entries.take(pageSize).reverse
    }
    if (feedEntries.size > 0 && feedEntries.head.sequenceNr != entries.last.sequenceNr)
      previousId = Some(entries.head.sequenceNr)

    ProcessedFeedEntries(previousId, feedEntries, nextId)
  }

  private[this] def processBackwardEntries(start: Long,
                                           pageSize: Int,
                                           entries: List[FeedEntry]): ProcessedFeedEntries = {
    require(entries != Nil)

    var nextId: Option[Long] = None
    var previousId: Option[Long] = None
    var feedEntries: List[FeedEntry] = Nil

    //backward => entries are sorted by id descending
    if (start == entries.head.sequenceNr) {
      // exclude start
      feedEntries = entries.tail.take(pageSize)
      previousId = Some(start) //previousId is start
    } else {
      //there is no next page
      feedEntries = entries.take(pageSize)
    }
    if (feedEntries.size > 0 && feedEntries.last.sequenceNr != entries.last.sequenceNr)
      nextId = Some(entries.last.sequenceNr)

    ProcessedFeedEntries(previousId, feedEntries, nextId)
  }

  /**
   *
   * @param pageSize the desired feed page size
   * @param entries the entries to include in the feed
   * @param previousEntryId the previous entry's id or None if we are at the head of the feed
   * @param nextEntryId the next entry's id or None if we are at the tail of the feed (last page)
   * @return a page feed or None
   */
  private[this] def toFeed(pageSize: Int,
                           entries: List[FeedEntry],
                           previousEntryId: Option[Long],
                           nextEntryId: Option[Long])(implicit context: C): Option[Feed[E]] = {

    for {
      entries <- Some(entries); if entries.size > 0
    } yield Feed[E](
      id = feedName,
      base = url,
      title = title,
      updated = entries.head.entry.updated,
      links = List(Link(Link.selfLink, feedLink(nextEntryId.getOrElse(minId), pageSize, forward = true)),
        Link(Link.lastLink, feedLink(minId, pageSize, forward = true))) ++
        nextEntryId.map { _ =>
          link(Link.nextLink, entries.last.sequenceNr, pageSize, forward = false)
        } ++
        previousEntryId.map { _ =>
          link(Link.previousLink, entries.head.sequenceNr, pageSize, forward = true)
        },
      entries = entries.map(_.entry)
    )
  }


  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize the maximum number of feed entries to return. The page could contain less entries
   * @return the head of the feed
   */
  override def getHeadOfFeed(pageSize: Int)(implicit context: C): Option[Feed[E]] = {

    require(pageSize > 0, "page size must be greater than 0")

    //fetch most recent entries from feed, we ask for one more than the pageSize to check if we are on the last page
    val entries: List[FeedEntry] = getMostRecentFeedEntries(pageSize + 1)

    if (entries.size > 0) {
      //we possibly need to return less entries to keep paging consistent => paging from tail to head or vice versa
      //must return the same pages in order to have efficient caching
      val n = (getNumberOfEntriesLowerThan(entries.head.sequenceNr) % pageSize).toInt
      val limit = if (n == 0) pageSize else n

      toFeed(pageSize,
        entries.take(limit),
        None,
        entries.drop(limit) match {
          case Nil    => None
          case h :: _ => Some(h.sequenceNr)
        }
      )

    } else {
      None
    }
  }

  /**
   * @return one less than the minimum sequence number used in this feed
   */
  def minId(implicit context: C): Long

  /**
   * @return the maximum sequence number used in this feed or minId if feed is empty
   */
  def maxId(implicit context: C): Long

  /**
   * @param sequenceNr sequence number to match
   * @param inclusive if true include the specified sequence number
   * @return the number of entries in the feed with sequence number lower than specified
   */
  def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true)(implicit context: C): Long

  /**
   * Retrieves the most recent entries from the `FeedStore` sorted in descending order
   * @param count the amount of recent entries to return
   * @return a list of FeedEntries. a FeedEntry is a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  def getMostRecentFeedEntries(count: Int)(implicit context: C): List[FeedEntry]

  /**
   * Retrieves entries with their sequence numbers from the feed
   *
   * @param start the starting entry (inclusive), MUST be returned in the entries
   * @param count the number of entries to return
   * @param ascending if true return entries with sequence numbers >= start in ascending order
   *                  else return entries with sequence numbers <= start in descending order
   * @return the corresponding entries sorted accordingly
   */
  def getFeedEntries(start: Long, count: Int, ascending: Boolean)(implicit context: C): List[FeedEntry]

  protected def getNextLink(id: Long, count: Int, next: Option[Long]): Option[Link] = {
    next.map { _ =>
      link(Link.nextLink, id, count, forward = false)
    }
  }

  protected def getPreviousLink(id: Long, count: Int, previous: Option[Long]): Option[Link] = {
    previous.map { _ =>
      link(Link.previousLink, id, count, forward = true)
    }
  }

  protected def link(l: String, start: Long, pageSize: Int, forward: Boolean): Link = {
    Link(l, feedLink(start, pageSize, forward))
  }

  protected def generateEntryID(): String = {
    s"urn:uuid:${UUID.randomUUID().toString}"
  }

  /**
   * Creates a link to a feed page.
   *
   * @param startId the starting entry's id (non inclusive)
   * @param count the number of entries in the page
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else navigate to 'next' elements in feed (towards last page of feed)
   * @return the URL
   */
  protected def feedLink(startId:Long, count: Int, forward: Boolean): Url = {
    val direction = if (forward) "forward" else "backward"
    Url(startId.toString) / direction / count.toString
  }

  protected case class FeedEntry(sequenceNr: Long, entry: Entry[E])

  private[this] case class ProcessedFeedEntries(previousSequenceNr: Option[Long],
                                                feedEntries: List[FeedEntry],
                                                nextSequenceNr: Option[Long])

}
