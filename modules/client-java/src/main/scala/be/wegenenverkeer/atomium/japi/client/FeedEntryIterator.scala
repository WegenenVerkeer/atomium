package be.wegenenverkeer.atomium.japi.client

import be.wegenenverkeer.atomium.client.EntryRef
import be.wegenenverkeer.atomium.client.FeedEntryIterator.Implicits._


class FeedEntryIterator[E](provider: FeedProvider[E], timeout: Long, initialEntryRefOpt: Option[EntryRef[E]])
  extends java.util.Iterator[EntryRef[E]] {


  def this(provider: FeedProvider[E], timeout: Long) =
    this(provider, timeout, None)

  def this(provider: FeedProvider[E]) =
    this(provider, 500, None)

  def this(provider: FeedProvider[E], initialEntryRef: EntryRef[E]) =
    this(provider, 500, Option(initialEntryRef))

  def this(provider: FeedProvider[E], timeout: Long, initialEntryRef: EntryRef[E]) =
    this(provider, timeout, Option(initialEntryRef))

  private val underlyingIterator = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    new FeedProviderWrapper(provider).iterator(timeout seconds, initialEntryRefOpt)
  }

  override def hasNext: Boolean = underlyingIterator.hasNext

  override def next(): EntryRef[E] = underlyingIterator.next()
}
