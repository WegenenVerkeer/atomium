package be.wegenenverkeer.atom

import com.typesafe.scalalogging.slf4j.Logging
import resource.Resource

import scala.util.Try

/** Fetches a feed page and provides its to the requester. */
object FeedPageProvider extends Logging {
  implicit def managedFeedProvider[T](provider: FeedPageProvider[T]): Resource[FeedPageProvider[T]] = new Resource[FeedPageProvider[T]] {
    override def open(r: FeedPageProvider[T]): Unit = {
      logger.debug(s"Opening ${r.getClass.getSimpleName} ... ")
      provider.start()
    }

    override def close(r: FeedPageProvider[T]): Unit = {
      logger.debug(s"Closing ${r.getClass.getSimpleName} ...")
      provider.stop()
    }
  }
}

trait FeedPageProvider[T] {

  def initialPosition: Option[FeedPosition]

  /**
   * Fetch the first page of the feed.
   *
   * @return the first page of the feed.
   */
  def fetchFeed(): Try[Feed[T]]

  /**
   * Fetch a specific page of the feed.
   *
   * @param page the page
   * @return a page of the feed
   */
  def fetchFeed(page: String): Try[Feed[T]]

  /**
   * This method is called when the feed processor is started.
   *
   * Implementations of this method can include any setup logic here.
   */
  def start(): Unit

  /**
   * This method is called when the feed processor is stopped.
   *
   * Implementations of this method can include any cleanup logic here.
   */
  def stop(): Unit
}