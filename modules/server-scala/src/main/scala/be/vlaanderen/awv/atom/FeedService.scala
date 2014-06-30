package be.vlaanderen.awv.atom

import org.joda.time.{DateTime, LocalDateTime}

import scala.annotation.tailrec

/**
 * A feed service is responsible for atom feeds for elements of type `E`.
 *
 * A feed service can be used to
 *  - add new elements to the feed
 *  - retrieve a page of the feed
 *
 * A feed service is not responsible for persistence of the entries and pages in feeds, this is
 * delegated to an instance of [[be.vlaanderen.awv.atom.FeedStore]].
 * 
 * Adding new elements to a specific feed is not thread safe!
 *
 * @param feedStore responsible for persisting feeds
 * @param entriesPerPage number of entries per feed page
 * @param title the title of the feed
 * @tparam E type of the elements in the feed
 */
class FeedService[E](feedStore: FeedStore[E], entriesPerPage: Int, title: String) {

  /**
   * Adds new elements to the feed.
   * 
   * Adding new elements to a specific feed is not thread safe!
   *
   * @param elements the elements to add
   */
  def push(elements: Iterable[E]) {
    val feedInfo = feedStore.getFeedInfo
    val (feedUpdates, updatedFeedInfo) = FeedService.determineFeedUpdates(elements, title, entriesPerPage, feedInfo)
    feedStore.update(feedUpdates, updatedFeedInfo)
  }

  /**
   * Adds a new element to the feed.
   *
   * Adding a new element to a specific feed is not thread safe!
   *
   * @param element the element to add
   */
  def push(element: E): Unit = push(List(element))

  /**
   * Retrieves a page of the feed.
   *
   * @param page the page number
   * @return the feed page or `None` if the page is not found
   */
  def getFeed(page: Long) = feedStore.getFeed(page)

  /**
   * This method should be called at startup of the application.
   *
   * This can be used to check integrity of the feeds.
   */
  def start = {
    feedStore.open
  }

  /**
   * This method should be called at shutdown of the application.
   */
  def stop = {
    feedStore.close
  }

}

object FeedService {

  /**
   * Determines the resulting updates when adding new elements to a feed.
   *
   * @param elements the new elements to add
   * @param entriesPerPage number of entries per feed page
   * @param feedInfo the feed info
   * @tparam E type of the elements in the feed
   * @return a list of [[be.vlaanderen.awv.atom.FeedUpdateInfo]]'s
   */
  protected[atom] def determineFeedUpdates[E](elements: Iterable[E], title: String, entriesPerPage: Int, feedInfo: Option[FeedInfo]) = {
    val firstPage = 1

    // Partitions new elements in lists of elements, the first list contains the elements for the current page
    // the other list contains elements for new feed pages
    @tailrec def partitionElements(elements: Iterable[E], currentFeedCount: Int, result: List[List[E]]): List[List[E]] = {
      if (elements.isEmpty) {
        return Nil
      }

      val count = entriesPerPage - currentFeedCount // number of entries that can be added to current page
      val eventsForPage = (elements take count).toList
      val eventsTodo = elements drop count

      if (eventsTodo.size > 0)
        partitionElements(eventsTodo, 0, result :+ eventsForPage)
      else
        result :+ eventsForPage
    }

    def calculatePreviousPage(current: Long) = current - 1
    def calculateNextPage(current: Long) = current + 1

    def determinePreviousPage(current: Option[Long]): Option[Long] = for {
      cur <- current
      prev <- Some(calculatePreviousPage(cur))
      if prev >= firstPage
    } yield prev

    val elementsPerFeed = {
      val feeds = partitionElements(elements, feedInfo map (_.count) getOrElse 0, Nil)
      if (feeds.isEmpty && feedInfo.isEmpty) {
        // if there is no page yet and there are no new elements being added, we still create an empty page
        List(Nil)
      } else {
        feeds
      }
    }

    // TODO refactor the algorithm below

    var currentPage = feedInfo map (_.lastPage) getOrElse 1L
    var previousPage = determinePreviousPage(feedInfo map(_.lastPage))

    val nextElementsPerFeed = ((elementsPerFeed drop 1) map (e => Some(e))) ++ List(None)

    val updates = collection.mutable.ListBuffer.empty[FeedUpdateInfo[E]]
    previousPage foreach { page =>
      val feedUpdate = FeedUpdateInfo(page, title, new DateTime(), false, List.empty[E],
        firstPage, determinePreviousPage(Some(page)), Some(currentPage))
      updates += feedUpdate
    }

    var countInFeed = feedInfo map (_.count) getOrElse 0

    val feedUpdates = (elementsPerFeed zip nextElementsPerFeed).foldLeft(updates) {
      case (feedUpdates, elements) =>
        val (current, next) = elements
        val feedUpdate = FeedUpdateInfo(
          page = currentPage,
          title = title,
          updated = new DateTime(),
          isNew = !(feedInfo.isDefined && currentPage == (feedInfo.get.lastPage)),
          newElements = current,
          first = firstPage,
          previous = previousPage,
          next = next map(_ => calculateNextPage(currentPage))
        )
        countInFeed = current.size + (if (feedInfo.isDefined && currentPage == (feedInfo.get.lastPage))
          feedInfo.get.count
        else
          0)
        previousPage = Some(currentPage)
        currentPage = calculateNextPage(currentPage)
        feedUpdates += feedUpdate
    }

    val tr = FeedInfo(
      count = countInFeed,
      lastPage = feedUpdates.lastOption map(_.page) getOrElse currentPage
    )

    (feedUpdates.toList, tr)
  }
}