package be.vlaanderen.awv.atom

/**
 * Data structure that contains the necessary information for a specific feed.
 *
 * @param count the number of elements contained in the last page of the feed
 * @param lastPage number of last page in the feed
 */
case class FeedInfo(
  count: Int,
  lastPage: Long
)
