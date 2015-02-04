package be.wegenenverkeer.atomium.client

import be.wegenenverkeer.atomium.client.async.{AsyncFeedEntryIterator, AsyncFeedProvider}
import be.wegenenverkeer.atomium.format.Feed

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


/** A blocking Feed Iterator.
  *
  * This Iterator is backed by an `AsyncFeedEntryIterator` and an `AsyncFeedProvider`
  *
  * @param feedProvider - a [[FeedProvider]] configured to fetch `Feed` pages
  * @param timeout - the maximum duration for waiting for `Feed` pages
  * @param execContext - an `ExecutionContext`
  * @tparam E - the Feed's content type
  */
class FeedEntryIterator[E](feedProvider: FeedProvider[E],
                           timeout: Duration,
                           execContext: ExecutionContext) extends Iterator[EntryRef[E]] {

  implicit val ec = execContext

  private val asyncFeedProvider = new AsyncFeedProvider[E] {

    override def initialEntryRef: Option[EntryRef[E]] = feedProvider.initialEntryRef

    override def fetchFeed(): Future[Feed[E]] = Future(feedProvider.fetchFeed().get)

    override def fetchFeed(pageUrl: String): Future[Feed[E]] = Future(feedProvider.fetchFeed(pageUrl).get)

  }

  /** The underlying `AsyncFeedEntryIterator` */
  val asyncIterator = new AsyncFeedEntryIterator(asyncFeedProvider, timeout)

  override def hasNext: Boolean = Await.result(asyncIterator.hasNext, timeout)

  override def next(): EntryRef[E] = asyncIterator.next()
}

object FeedEntryIterator {

  object Implicits {

    implicit class IteratorBuilder[T](feedProvider: FeedProvider[T]) {

      /** Returns a `FeedEntryIterator` configured with the default scala `ExecutionContext` and a timeout of 500 millis */
      def iterator(): FeedEntryIterator[T] = {
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.duration._
        iterator(500 millis)
      }

      def iterator(timeout: Duration)(implicit executionContext: ExecutionContext): FeedEntryIterator[T] = {
        new FeedEntryIterator(feedProvider, timeout, executionContext)
      }

    }

  }

}


