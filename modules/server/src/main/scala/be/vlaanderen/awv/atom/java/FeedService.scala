package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.Context
import scala.collection.JavaConverters._

/**
 * Wrapper wround the [[be.vlaanderen.awv.atom.FeedService]] that offers a Java-like interface.
 *
 * @param feedName the name of this feed, which can be used as an identifier for the feed
 * @param entriesPerPage the number of entries per page
 * @param title the feed title
 * @param feedStoreFactory a factory for creating feed stores*
 * @param context the context, which is required for feed stores
 *
 * @tparam E the type of the feed entries
 * @tparam C the type of the context, which is required for feed stores
 */
class FeedService[E, C <: Context](context: C, feedName: String, entriesPerPage: Integer, title: String, feedStoreFactory: FeedStoreFactory[E, C]) {
  private val underlying: be.vlaanderen.awv.atom.FeedService[E, C] =
    new be.vlaanderen.awv.atom.FeedService[E, C](feedName, entriesPerPage, title,
      (name, context) => feedStoreFactory.create(name, context))

  /**
   * Adds elements to the feed.
   *
   * @param elements the elements to add
   */
  def push(elements: java.lang.Iterable[E]) = underlying.push(elements.asScala)(context)

  /**
   * Adds an element to the feed.
   *
   * @param element the element to add
   */
  def push(element: E) = underlying.push(element)(context)

  /**
   * Retrieves a feed page.
   * @param start the starting entry
   * @param count the number of entries
   * @return the feed page
   */
  def getFeed(start:Int, count:Int) = underlying.getFeedPage(start, count)(context)
}
