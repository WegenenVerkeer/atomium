package be.wegenenverkeer.atomium.japi.server

import java.lang.{Boolean => JBoolean, Long => JLong}

import be.wegenenverkeer.atomium.format.JFeedConverters._
import be.wegenenverkeer.atomium.japi.format.Feed
import be.wegenenverkeer.atomium.server
import be.wegenenverkeer.atomium.server.Context

import scala.collection.JavaConverters._

/**
 * Wrapper around the [[FeedService]] that offers a Java-like interface.
 *
 * @param feedName the name of this feed, which can be used as an identifier for the feed
 * @param entriesPerPage the number of entries per page
 * @param feedStoreFactory a factory for creating feed stores*
 * @param context the context, which is required for feed stores
 *
 * @tparam E the type of the feed entries
 * @tparam C the type of the context, which is required for feed stores
 */
class FeedService[E, C <: Context](context: C, feedName: String, entriesPerPage: Integer, feedStoreFactory: FeedStoreFactory[E, C]) {

  private val underlying: server.FeedService[E, C] =
    new server.FeedService[E, C](feedName, entriesPerPage, (name, context) => feedStoreFactory.create(name, context))

  /**
   * Adds elements to the feed.
   *
   * @param elements the elements to add
   */
  def push(elements: java.lang.Iterable[E]): Unit = underlying.push(elements.asScala)(context)

  /**
   * Adds an element to the feed.
   *
   * @param element the element to add
   */
  def push(element: E): Unit = underlying.push(element)(context)

  /**
   * Retrieves a feed page.
   * @param start the starting entry
   * @param count the number of entries
   * @return the feed page
   */
  def getFeed(start: JLong, count: Integer, forward: JBoolean): Option[Feed[E]] =
    underlying.getFeedPage(start, count, forward)(context).map(feed2JFeed)

}
