package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.java.{FeedProvider => JFeedProvider}
import be.vlaanderen.awv.atom.{Feed, FeedPosition, JFeedConverters}
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.Try

/**
 * Wrapper around the [[be.vlaanderen.awv.atom.FeedProvider]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[be.vlaanderen.awv.atom.FeedProvider]]
 * @tparam E the type of the entries in the feed
 */
class FeedProviderWrapper[E](underlying: JFeedProvider[E])
  extends be.vlaanderen.awv.atom.FeedProvider[E] with Logging {

  override def fetchFeed(): Try[Feed[E]] =
    Try(JFeedConverters.jFeed2Feed(underlying.fetchFeed))

  def fetchFeed(page: String): Try[Feed[E]] =
    Try (JFeedConverters.jFeed2Feed(underlying.fetchFeed(page)))

  override def start(): Unit = underlying.start()

  override def stop(): Unit = underlying.stop()

  override def initialPosition: Option[FeedPosition] = Option.apply(underlying.getInitialPosition)
}