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
                           execContext: ExecutionContext,
                           initialEntryRef: Option[EntryRef[E]] = None) extends Iterator[EntryRef[E]] {

  implicit val ec = execContext

  private val asyncFeedProvider = new AsyncFeedProvider[E] {

    override def fetchFeed(initialEntryRef: Option[EntryRef[E]] = None): Future[Feed[E]] =
      Future(feedProvider.fetchFeed(initialEntryRef).get)

    override def fetchFeed(pageUrl: String): Future[Feed[E]] =
      Future(feedProvider.fetchFeed(pageUrl).get)

  }

  /** The underlying `AsyncFeedEntryIterator` */
  val asyncIterator = new AsyncFeedEntryIterator(asyncFeedProvider, timeout, initialEntryRef)

  override def hasNext: Boolean = Await.result(asyncIterator.hasNext, timeout)

  override def next(): EntryRef[E] = asyncIterator.next()
}

object FeedEntryIterator {

  object Implicits {

    implicit class IteratorBuilder[E](feedProvider: FeedProvider[E]) {

      def iterator(): FeedEntryIterator[E] = {
        iterator(None)
      }

      /** Returns a `FeedEntryIterator` configured with the default scala `ExecutionContext` and a timeout of 500 millis */
      def iterator(initialEntryRef: Option[EntryRef[E]]): FeedEntryIterator[E] = {
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.duration._
        iterator(500 millis, initialEntryRef)
      }

      def iterator(timeout: Duration, initialEntryRef: Option[EntryRef[E]])
                  (implicit executionContext: ExecutionContext): FeedEntryIterator[E] = {
        new FeedEntryIterator(feedProvider, timeout, executionContext, initialEntryRef)
      }

    }

  }

}


