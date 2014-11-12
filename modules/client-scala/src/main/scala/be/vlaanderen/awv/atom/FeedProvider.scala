package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.Feed
import com.typesafe.scalalogging.slf4j.Logging
import resource.Resource
import scala.language.implicitConversions
import scala.util.Try

/**
 * A feed provider is responsible for providing the feed pages.
 *
 * Currently, Atomium comes with 1 implementation out-of-the-box, a provider that uses the Play WS API for fetching feed
 * pages via HTTP, [[be.vlaanderen.awv.atom.providers.PlayWsBlockingFeedProvider]].
 *
 * When fetching the feed pages, a feed provider should return a [[scala.util.Try]] instead of throwing an exception.
 *
 * @tparam T the type of the entries in the feed
 */
trait FeedProvider[T]  {
  def initialPosition: Option[FeedPosition]

  /**
   * Fetch the first page of the feed.
   *
   * @return the first page of the feed.
   */
  def fetchFeed() : Try[Feed[T]]

  /**
   * Fetch a specific page of the feed.
   *
   * @param page the page
   * @return a page of the feed
   */
  def fetchFeed(page:String) : Try[Feed[T]]

  /**
   * This method is called when the feed processor is started.
   *
   * Implementations of this method can include any setup logic here.
   */
  def start() : Unit

  /**
   * This method is called when the feed processor is stopped.
   *
   * Implementations of this method can include any cleanup logic here.
   */
  def stop() : Unit
}


object FeedProvider extends Logging {
  implicit def managedFeedProvider[T](provider : FeedProvider[T]) : Resource[FeedProvider[T]] = new Resource[FeedProvider[T]] {
    override def open(r: FeedProvider[T]): Unit = {
      logger.debug(s"Opening ${r.getClass.getSimpleName} ... ")
      provider.start()
    }
    override def close(r: FeedProvider[T]): Unit = {
      logger.debug(s"Closing ${r.getClass.getSimpleName} ...")
      provider.stop()
    }
  }
}