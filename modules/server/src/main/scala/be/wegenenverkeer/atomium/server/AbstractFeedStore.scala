package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.format.{Feed, Url}

/**
 * A feed store is responsible for the persistence of feeds.
 * This abstract class serves as a base class for more specific FeedStore implementations.
 *
 * @tparam E type of the elements in the feed
 */
abstract class AbstractFeedStore[E, C <: Context](feedName: String,
                                                  title: Option[String],
                                                  url: Url) extends FeedStore[E, C] with FeedStoreSupport[E] {

  implicit val feedParams = FeedParams(feedName, url, title)

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
      processFeedEntries(start, minId, pageSize, forward, getFeedEntries(start, pageSize + 2, forward))
    } else {
      None
    }
  }

  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize the maximum number of feed entries to return. The page could contain less entries
   * @return the head of the feed
   */
  override def getHeadOfFeed(pageSize: Int)(implicit context: C): Option[Feed[E]] = {

    require(pageSize > 0, "page size must be greater than 0")

    //fetch most recent entries from feed, we ask for one more than the pageSize to check if we are on the last page
    val entries = getMostRecentFeedEntries(pageSize + 1)

    if (entries.nonEmpty) {
      processHeadFeedEntries(getNumberOfEntriesLowerThan(entries.head.sequenceNr), minId, pageSize, entries)
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

}
