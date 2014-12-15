package be.wegenenverkeer.atom

import org.joda.time.LocalDateTime

import scala.collection.immutable.TreeMap

class TestFeedStore[T] extends AbstractFeedStore[T](
  "test_store",
  None,
  new UrlBuilder {
    override def base: Url = Url("http://www.example.org/testfeed")
    override def collectionLink: Url = ???
  }) {

  var skip = 0
  var nextSequenceNum = 0L
  var entriesMap : TreeMap[Long, Entry[T]] = TreeMap.empty[Long, Entry[T]]

  override def context: Context = ???

  def sequenceNumbersToSkipForPush(skip: Int): Unit = {
    require(skip >= 0)
    this.skip = skip
  }

  override def push(entries: Iterable[T]): Unit = {
    entries foreach { e =>
      nextSequenceNum += (skip + 1)
      entriesMap += (nextSequenceNum -> Entry("id", new LocalDateTime(), Content(e, ""), Nil))
    }
    sequenceNumbersToSkipForPush(0)
  }

  override val minId: Long = 0

  override def maxId: Long =
    if (entriesMap.keys.size > 0)
      entriesMap.keys.max
    else
      minId

  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true): Long =
    if (inclusive)
      entriesMap.count(_._1 <= sequenceNr)
    else
      entriesMap.count(_._1 < sequenceNr)

  override def getFeedEntries(start: Long, pageSize: Int, forward: Boolean): List[(Long, Entry[T])] = {
    if (forward)
      entriesMap.dropWhile(_._1 < start).take(pageSize).toList
    else
      entriesMap.takeWhile(_._1 <= start).toList.reverse.take(pageSize)
  }

  override def getMostRecentFeedEntries(count: Int) = {
    entriesMap.toList.reverse.take(count)
  }

  override def toString: String = {
    entriesMap.toString()
  }
}

