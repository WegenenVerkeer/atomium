package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.api.FeedPage
import be.wegenenverkeer.atomium.format.Url

import scala.concurrent.{ExecutionContext, Future}

/**
 * A feed store is responsible for the persistence of feeds.
 * This abstract class serves as a base class for more specific FeedStore implementations.
 *
 * @tparam E type of the elements in the feed
 */
abstract class AbstractAsyncFeedStore[E, C <: Context](feedName: String,
                                                       title: Option[String],
                                                       url: Url) extends AsyncFeedStore[E, C] with FeedStoreSupport[E] {


  implicit val feedParams = FeedParams(feedName, url, title)

  /**
   * Retrieves a page of the feed.
   *
   * @param start the starting entry (exclusive), should not be returned in the feed page
   * @param pageSize the number of entries
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else navigate to 'next' elements in feed (towards last page of feed)
   * @return the feed page or `Future.failed` if the page is not found
   */
  override def getFeed(start: Long, pageSize: Int, forward: Boolean)
                      (implicit executionContext: ExecutionContext, context: C): Future[Option[FeedPage[E]]] = {
    require(pageSize > 0)

    val allowedFuture: Future[Boolean] = for {
      max          <- maxId
      lowerEntries <- getNumberOfEntriesLowerThan(start, forward)
    } yield start == 0 || (start <= max && lowerEntries % pageSize == 0)


    for {
      allowed <- allowedFuture
      entries <- if (allowed) { getFeedEntries(start, pageSize + 2, forward) } else { Future.successful(List.empty) }
      min     <- minId
    } yield Some(processFeedEntries(start, min, pageSize, forward, entries))

  }



  /**
   * Retrieves the head of the feed. This is the first page containing the most recent entries
   * @param pageSize the maximum number of feed entries to return. The page could contain less entries
   * @return the head of the feed
   */
  override def getHeadOfFeed(pageSize: Int)
                            (implicit executionContext: ExecutionContext, context: C): Future[FeedPage[E]] = {

    require(pageSize > 0, "page size must be greater than 0")

    //fetch most recent entries from feed, we ask for one more than the pageSize to check if we are on the last page
    for {
      entries         <- getMostRecentFeedEntries(pageSize + 1)
      numberOfEntries <- if (entries.nonEmpty) getNumberOfEntriesLowerThan(entries.head.sequenceNr) else Future.successful(0L)
      min             <- minId
    } yield processHeadFeedEntries(numberOfEntries, min, pageSize, entries)

  }

  /**
   * @return one less than the minimum sequence number used in this feed
   */
  def minId(implicit context: C): Future[Long]

  /**
   * @return the maximum sequence number used in this feed or minId if feed is empty
   */
  def maxId(implicit context: C): Future[Long]

  /**
   * @param sequenceNr sequence number to match
   * @param inclusive if true include the specified sequence number
   * @return the number of entries in the feed with sequence number lower than specified
   */
  def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true)
                                 (implicit executionContext: ExecutionContext, context: C): Future[Long]

  /**
   * Retrieves the most recent entries from the `FeedStore` sorted in descending order
   * @param count the amount of recent entries to return
   * @return a list of FeedEntries. a FeedEntry is a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  def getMostRecentFeedEntries(count: Int)
                              (implicit executionContext: ExecutionContext, context: C): Future[List[FeedStoreSupport[E]#FeedEntry]]

  /**
   * Retrieves entries with their sequence numbers from the feed
   *
   * @param start the starting entry (inclusive), MUST be returned in the entries
   * @param count the number of entries to return
   * @param ascending if true return entries with sequence numbers >= start in ascending order
   *                  else return entries with sequence numbers <= start in descending order
   * @return the corresponding entries sorted accordingly
   */
  def getFeedEntries(start: Long, count: Int, ascending: Boolean)
                    (implicit executionContext: ExecutionContext, context: C): Future[List[FeedStoreSupport[E]#FeedEntry]]

}
