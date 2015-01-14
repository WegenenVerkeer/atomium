package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.async.{AsyncFeedEntryIterator, AsyncFeedProvider}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class FeedEntryIterator[E] (feedProvider: FeedProvider[E], timeout:Duration)(implicit val execContext:ExecutionContext)
  extends Iterator[EntryLoc[E]] {


  val asyncFeedProvider = new AsyncFeedProvider[E] {

    override def initialEntryRef: Option[EntryRef[E]] = feedProvider.initialEntryRef

    override def fetchFeed(): Future[Feed[E]] = Future(feedProvider.fetchFeed().get)

    override def fetchFeed(pageUrl: String): Future[Feed[E]] = Future(feedProvider.fetchFeed(pageUrl).get)
    
  }
  
  val asyncIterator = new AsyncFeedEntryIterator(asyncFeedProvider, timeout)

  override def hasNext: Boolean = Await.result(asyncIterator.hasNext, timeout)

  override def next(): EntryLoc[E] = asyncIterator.next()
}

object FeedEntryIterator {
  object Implicits {
    implicit class IteratorBuilder[T](feedProvider: FeedProvider[T]) {
      def iterator(timeout:Duration)(implicit ec: ExecutionContext) = new FeedEntryIterator(feedProvider, timeout)
    }
  }
}


