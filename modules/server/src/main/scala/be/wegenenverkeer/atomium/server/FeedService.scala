package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.format.Feed

/**
 * A feed service provides the following functionality:
 * - push new entries to the feed
 * - get a page from the feed
 *
 * @param entriesPerPage the number of entries per page
 * @param feedStore a feed store
 * @tparam E the type of the feed entries
 * @tparam C the type of the context, which is required for feed stores
 */
class FeedService[E, C <: Context](entriesPerPage: Int, feedStore: FeedStore[E, C]) {

  /**
   * Adds elements to the feed.
   *
   * @param elements the elements to add
   * @param context the context, which is required for feed stores
   */
  def push(elements: Iterable[E])(implicit context: C): Unit = {
    feedStore.push(elements)
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
   * Adds an element to the feed with a given uuid.
   *
   * @param uuid the uuid to use as the feed entry id to which the element is added
   * @param element the element to add
   * @param context the context, which is required for feed stores
   */
  def push(uuid: String, element: E)(implicit context: C): Unit = {
    feedStore.push(uuid, element)
  }

  /**
   * Retrieves a feed page
   * @param startSequenceNr start feed from entry with this sequence number
   * @param pageSize number of entries to return in feed page
   * @param context to retrieve the feed page
   * @return a feed page or None if the startSequenceNr and pageSize are incorrect,
   *         for example when these are arbitrarily chosen by atom client,
   *         because this defeats the caching heuristics.
   *         atom clients should navigate using the links in the atom feed
   */
  def getFeedPage(startSequenceNr: Long, pageSize: Int, forward: Boolean)(implicit context: C): Option[Feed[E]] = {
    if (pageSize == entriesPerPage) {
      feedStore.getFeed(startSequenceNr, pageSize, forward)
    } else {
      None
    }
  }

  /**
   * Retrieves the head of the feed
   * @param context the context, which is required for feed stores
   * @return the head of the feed. This is the first page containing the most recent entries
   */
  def getHeadOfFeed()(implicit context: C): Feed[E] = {
    feedStore.getHeadOfFeed(entriesPerPage)
  }


}
