package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.EntryRef
import be.wegenenverkeer.atomium.format
import be.wegenenverkeer.atomium.format.{JFeedConverters, Feed}
import scala.util.Try

/**
 * Wrapper around the [[be.wegenenverkeer.atom.java.FeedProvider]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[be.wegenenverkeer.atom.java.FeedProvider]]
 * @tparam E the type of the entries in the feed
 */
class FeedProviderWrapper[E](underlying: be.wegenenverkeer.atom.java.FeedProvider[E])
  extends be.wegenenverkeer.atom.FeedProvider[E] {

  override def fetchFeed(): Try[format.Feed[E]] =
    Try(JFeedConverters.jFeed2Feed(underlying.fetchFeed()))

  def fetchFeed(page: String): Try[Feed[E]] =
    Try (JFeedConverters.jFeed2Feed(underlying.fetchFeed(page)))
  
  override def initialEntryRef: Option[EntryRef[E]] = Option.apply(underlying.getInitialEntryRef)
}