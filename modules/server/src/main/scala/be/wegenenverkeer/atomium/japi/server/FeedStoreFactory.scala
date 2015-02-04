package be.wegenenverkeer.atomium.japi.server

import be.wegenenverkeer.atomium.server.Context

/**
 * Responsible for creating feed stores
 *
 * @tparam E the type of the feed entries
 * @tparam C the type of the context, which is required for feed stores
 */
trait FeedStoreFactory[E, C <: Context] {

  /**
   * Creates a new feed store
   *
   * @param feedName the name of the feed, which can be used as an identifier for the feed
   * @param context the feed store context
   *
   * @return a new feed store
   */
  def create(feedName: String, context: C): FeedStore[E]

}
