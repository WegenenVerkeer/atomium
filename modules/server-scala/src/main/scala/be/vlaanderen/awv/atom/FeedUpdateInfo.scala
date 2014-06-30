package be.vlaanderen.awv.atom

import org.joda.time.{DateTime, LocalDateTime}

/**
 * Data structure that contains the necessary information to:
 *  - add elements to the last (= current) page
 *  - update the next page link (when a new page is added)
 *  - add new pages with new elements
 *
 * @param page the page number
 * @param title the title of the feed
 * @param updated the timestamp
 * @param isNew indicates if a new page should be created or not
 * @param newElements the new elements for the page
 * @param first number of the first page
 * @param previous number of the previous page
 * @param next number of the next page
 * @tparam E type of the elements in the feed
 */
case class FeedUpdateInfo[E](
  page: Long,
  title: String,
  updated: DateTime,
  isNew: Boolean,
  newElements: List[E],
  first: Long,
  previous: Option[Long],
  next: Option[Long]
)
