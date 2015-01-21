package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.async.{AsyncFeedEntryIterator, AsyncFeedProvider}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class FeedEntryIterator[E] (feedProvider: FeedProvider[E],
                            timeout:Duration,
                            execContext:ExecutionContext) extends Iterator[EntryRef[E]] {

  implicit val ec = execContext

  val asyncFeedProvider = new AsyncFeedProvider[E] {

    override def initialEntryRef: Option[EntryRef[E]] = feedProvider.initialEntryRef

    override def fetchFeed(): Future[Feed[E]] = Future(feedProvider.fetchFeed().get)

    override def fetchFeed(pageUrl: String): Future[Feed[E]] = Future(feedProvider.fetchFeed(pageUrl).get)
    
  }
  
  val asyncIterator = new AsyncFeedEntryIterator(asyncFeedProvider, timeout)

  override def hasNext: Boolean = Await.result(asyncIterator.hasNext, timeout)

  override def next(): EntryRef[E] = asyncIterator.next()
}

object FeedEntryIterator {
  object Implicits {
    implicit class IteratorBuilder[T](feedProvider: FeedProvider[T]) {

      def iterator(): FeedEntryIterator[T] = {

        import scala.concurrent.duration._
        val executionContext = scala.concurrent.ExecutionContext.global
        val timeout = 500 millis

        new FeedEntryIterator(feedProvider, timeout, executionContext)
      }

      def iterator(timeout:Duration, executionContext: ExecutionContext): FeedEntryIterator[T] = {
        new FeedEntryIterator(feedProvider, timeout, executionContext)
      }

      def iterator(timeout:Duration): FeedEntryIterator[T] = {
        val executionContext = scala.concurrent.ExecutionContext.global
        iterator(timeout, executionContext)
      }
    }
  }
}


