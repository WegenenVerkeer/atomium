package be.wegenenverkeer.atom

/**
 * A feed store is responsible for the persistence of feeds.
 * This abstract class serves as a base class for more specific FeedStore implementations.
 *
 * @tparam E type of the elements in the feed
 */
abstract class AbstractFeedStore[E](feedName: String,
                                    title: Option[String],
                                    urlProvider: UrlBuilder) extends FeedStore[E] {

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry (exclusive), should not be returned in the feed page
   * @param pageSize the number of entries
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else navigate to 'next' elements in feed (towards last page of feed)
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(start:Long, pageSize: Int, forward: Boolean): Option[Feed[E]] = {
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
        toFeed(pageSize, result._2, result._1, result._3)
      } else
        None
    } else {
      None
    }
  }

  private[this] def processForwardEntries(start: Long, pageSize: Int, entries: List[(Long, Entry[E])]):
  (Option[Long], List[(Long, Entry[E])], Option[Long]) = {
    require(entries != Nil)
    var nextId: Option[Long] = None
    var previousId: Option[Long] = None
    var feedEntries: List[(Long, Entry[E])] = Nil

    //entries are sorted by id ascending
    if (start == entries.head._1) { //start should be excluded
      feedEntries = entries.tail.take(pageSize).reverse
      nextId = Some(start) //nextId is start
    } else { //there is no next page
      feedEntries = entries.take(pageSize).reverse
    }
    if (feedEntries.size > 0 && feedEntries.head._1 != entries.last._1)
      previousId = Some(entries.head._1)

    (previousId, feedEntries, nextId)
  }

  private[this] def processBackwardEntries(start: Long, pageSize: Int, entries: List[(Long, Entry[E])]):
    (Option[Long], List[(Long, Entry[E])], Option[Long]) = {
    require(entries != Nil)
    var nextId: Option[Long] = None
    var previousId: Option[Long] = None
    var feedEntries: List[(Long, Entry[E])] = Nil

    //backward => entries are sorted by id descending
    if (start == entries.head._1) { // exclude start
      feedEntries = entries.tail.take(pageSize)
      previousId = Some(start) //previousId is start
    } else { //there is no next page
      feedEntries = entries.take(pageSize)
    }
    if (feedEntries.size > 0 && feedEntries.last._1 != entries.last._1)
      nextId = Some(entries.last._1)

    (previousId, feedEntries, nextId)
  }

  /**
   *
   * @param pageSize the desired feed page size
   * @param entries the entries to include in the feed
   * @param previousEntryId the previous entry's id or None if we are at the head of the feed
   * @param nextEntryId the next entry's id or None if we are at the tail of the feed (last page)
   * @return a page feed or None
   */
  protected def toFeed(pageSize: Int, 
                       entries: List[(Long, Entry[E])], 
                       previousEntryId: Option[Long], 
                       nextEntryId: Option[Long]): Option[Feed[E]] = {
    for {
      entries <- Some(entries); if entries.size > 0
    } yield Feed[E](
      id = feedName,
      base = urlProvider.base,
      title = title,
      updated = entries.head._2.updated,
      links = List(Link(Link.selfLink, urlProvider.feedLink(nextEntryId.getOrElse(minId), pageSize, forward = true)),
        Link(Link.lastLink, urlProvider.feedLink(minId, pageSize, forward = true))) ++
        nextEntryId.map { _ =>
          link(Link.nextLink, entries.last._1, pageSize, forward = false)
        } ++
        previousEntryId.map { _ =>
          link(Link.previousLink, entries.head._1, pageSize, forward = true)
        },
      entries = entries.map(_._2)
    )
  }


  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize the maximum number of feed entries to return. The page could contain less entries
   * @return the head of the feed
   */
  override def getHeadOfFeed(pageSize: Int): Option[Feed[E]] = {
    require(pageSize > 0)
    //fetch most recent entries from feed, we ask for one more than the pageSize to check if we are on the last page
    val entries: List[(Long, Entry[E])] = getMostRecentFeedEntries(pageSize+1)
    if (entries.size > 0) {
      //we possibly need to return less entries to keep paging consistent => paging from tail to head or vice versa
      //must return the same pages in order to have efficient caching
      val n = (getNumberOfEntriesLowerThan(entries.head._1) % pageSize).toInt
      val limit = if (n == 0) pageSize else n

      toFeed(pageSize,
        entries.take(limit),
        None,
        entries.drop(limit) match {
          case Nil => None
          case h :: _ => Some(h._1)
        }
      )

    } else {
      None
    }
  }

  /**
   * @return one less than the minimum sequence number used in this feed
   */
  def minId: Long

  /**
   * @return the maximum sequence number used in this feed or minId if feed is empty
   */
  def maxId: Long

  /**
   * @param sequenceNr sequence number to match
   * @param inclusive if true include the specified sequence number
   * @return the number of entries in the feed with sequence number lower than specified
   */
  def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true): Long

  /**
   * retrieves the most recent entries from the feedstore sorted in descending order
   * @param count the amount of recent entries to return
   * @return a list containing tuples of a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  def getMostRecentFeedEntries(count: Int): List[(Long, Entry[E])]

  /**
   * Retrieves entries with their sequence numbers from the feed
   *
   * @param start the starting entry (inclusive), MUST be returned in the entries
   * @param count the number of entries to return
   * @param ascending if true return entries with sequence numbers >= start in ascending order
   *                else return entries with sequence numbers <= start in descending order
   * @return the corresponding entries sorted accordingly
   */
  def getFeedEntries(start:Long, count: Int, ascending: Boolean): List[(Long, Entry[E])]

  protected def getNextLink(id: Long, count: Int, next: Option[Long]) : Option[Link] = {
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
    Link(l, urlProvider.feedLink(start, pageSize, forward))
  }

}
