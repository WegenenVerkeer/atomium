package be.wegenenverkeer.atomium.japi.client

import java.util

import be.wegenenverkeer.atomium.client.EntryRef

class FeedEntryIterable[E](provider: FeedProvider[E], timeout: Long, initialEntryRefOpt: Option[EntryRef[E]])
  extends java.lang.Iterable[EntryRef[E]] {


  def this(provider: FeedProvider[E], timeout: Long) =
    this(provider, timeout, None)

  def this(provider: FeedProvider[E]) =
    this(provider, 500, None)

  def this(provider: FeedProvider[E], initialEntryRef: EntryRef[E]) =
    this(provider, 500, Option(initialEntryRef))

  def this(provider: FeedProvider[E], timeout: Long, initialEntryRef: EntryRef[E]) =
    this(provider, timeout, Option(initialEntryRef))

  override def iterator(): util.Iterator[EntryRef[E]] = new FeedEntryIterator[E](provider, timeout, initialEntryRefOpt)
}
