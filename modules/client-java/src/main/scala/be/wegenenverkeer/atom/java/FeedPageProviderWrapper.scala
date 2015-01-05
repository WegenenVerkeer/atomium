package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.{FeedPosition, JFeedConverters}
import com.typesafe.scalalogging.slf4j.Logging

import scala.util.Try

/**
 * Wrapper around the [[be.wegenenverkeer.atom.java.FeedProvider]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[be.wegenenverkeer.atom.java.FeedProvider]]
 * @tparam E the type of the entries in the feed
 */
class FeedPageProviderWrapper[E](underlying: be.wegenenverkeer.atom.java.FeedProvider[E])
  extends be.wegenenverkeer.atom.FeedPageProvider[E] with Logging {

  override def fetchFeed(): Try[be.wegenenverkeer.atom.Feed[E]] =
    Try(JFeedConverters.jFeed2Feed(underlying.fetchFeed()))

  def fetchFeed(page: String): Try[be.wegenenverkeer.atom.Feed[E]] =
    Try (JFeedConverters.jFeed2Feed(underlying.fetchFeed(page)))

  override def start(): Unit = underlying.start()

  override def stop(): Unit = underlying.stop()

  override def initialPosition: Option[FeedPosition] = Option.apply(underlying.getInitialPosition)
}