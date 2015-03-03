package be.wegenenverkeer.atomium.japi.client

import be.wegenenverkeer.atomium.client.EntryRef
import be.wegenenverkeer.atomium.format.FeedConverters._

import scala.util.Try

/**
 * Wrapper around a [[be.wegenenverkeer.atomium.japi.client.FeedProvider]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[FeedProvider]]
 * @tparam E the type of the entries in the feed
 */
class FeedProviderWrapper[E](underlying: FeedProvider[E])
  extends be.wegenenverkeer.atomium.client.FeedProvider[E] {

  override def fetchFeed(initialEntryRef: Option[EntryRef[E]] = None): Try[be.wegenenverkeer.atomium.format.Feed[E]] = {

    val feed =
      initialEntryRef match {
        case Some(entryRef) => underlying.fetchFeed(entryRef)
        case None => underlying.fetchFeed()
      }

    Try(feed.asScala)
  }

  def fetchFeed(page: String): Try[be.wegenenverkeer.atomium.format.Feed[E]] =
    Try(underlying.fetchFeed(page).asScala)

}