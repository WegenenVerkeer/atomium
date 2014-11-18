package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.{FeedContent, Feed}

class FeedService[E <: FeedContent, C <: Context](feedName: String, entriesPerPage: Int, feedStoreFactory: (String, C) => FeedStore[E]) {

  /**
   * @param elements elements to push onto the feed
   * @param context to store the entries
   */
  def push(elements: Iterable[E])(implicit context: C): Unit = {
    feedStoreFactory(feedName, context).push(elements)
  }

  /**
   * @param element element to push onto the feed
   * @param context to store the entry
   * @return
   */
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
