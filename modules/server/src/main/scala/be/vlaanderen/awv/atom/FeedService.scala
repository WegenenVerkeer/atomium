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
    feedStoreFactory(feedName, context).push(elements)
  }

  def push(element: E)(implicit context: C): Unit = {
    push(List(element))(context)
  }

  /**
   * @param start start feed from entry
   * @param pageSize number of entries to return in feed page
   * @param context to retrieve the feed page
   * @return a feed page or None if the start and pageSize are incorrect, for example arbitrary chosen by atom client,
   *         because this defeats the caching heuristics. Clients should navigate using the links in the atom feed
   */
  def getFeedPage(start: Int, pageSize:Int)(implicit context: C):Option[Feed[E]] = {
    if (pageSize == entriesPerPage && start % pageSize == 0) {
      feedStoreFactory(feedName, context).getFeed(start, pageSize)
    } else {
      None
    }
  }

  /**
   * @param context to retrieve the feed page
   * @return the head of the feed. This is the first page containing the most recent entries
   */
  def getHeadOfFeed()(implicit context: C) : Option[Feed[E]] = {
    feedStoreFactory(feedName, context).getHeadOfFeed(entriesPerPage)
  }

}
