package be.vlaanderen.awv.atom

/**
 * A feed service provides the following functionality:
 *  - push new entries to the feed
 *  - get a page from the feed
 *
 * @param feedName the name of this feed, which can be used as an identifier for the feed
 * @param entriesPerPage the number of entries per page
 * @param title the feed title
 * @param feedStoreFactory a factory for creating feed stores
 * @tparam E the type of the feed entries
 * @tparam C the type of the context, which is required for feed stores
 */
class FeedService[E, C <: Context](feedName: String, entriesPerPage: Int, title: String, feedStoreFactory: (String, C) => FeedStore[E]) {

  /**
   * Adds elements to the feed.
   *
   * @param elements the elements to add
   * @param context the context, which is required for feed stores
   */
  def push(elements: Iterable[E])(implicit context: C): Unit = {
    val feedPusher = new FeedPusher[E](feedStoreFactory(feedName, context), entriesPerPage, title)
    feedPusher.push(elements)(context)
  }

  /**
   * Adds an element to the feed.
   *
   * @param element the element to add
   * @param context the context, which is required for feed stores
   */
  def push(element: E)(implicit context: C): Unit = {
    push(List(element))(context)
  }

  /**
   * Retrieves a feed page.
   *
   * @param page the page number
   * @param context the context, which is required for feed stores
   * @return the feed page
   */
  def getFeed(page:Long)(implicit context: C):Option[Feed[E]] = {
    val feedPusher = new FeedPusher[E](feedStoreFactory(feedName, context), entriesPerPage, title)
    feedPusher.getFeed(page)
  }

}
