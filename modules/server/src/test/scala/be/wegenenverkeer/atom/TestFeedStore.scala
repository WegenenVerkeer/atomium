package be.wegenenverkeer.atom

import be.wegenenverkeer.atomium.format.{Content, Entry, Url}

import scala.collection.immutable.TreeMap

import org.joda.time.DateTime

object TestFeedStore {

  val urlBuilder: UrlBuilder = new UrlBuilder {
    override def base: Url = Url("http://www.example.org/testfeed")
    override def collectionLink: Url = ???
  }

}

class TestFeedStore[T] extends AbstractFeedStore[T](
  "test_store",
  None,
  TestFeedStore.urlBuilder) {

  var skip = 0
  var nextSequenceNum = 0L
  var entriesMap : TreeMap[Long, Entry[T]] = TreeMap.empty[Long, Entry[T]]

  def sequenceNumbersToSkipForPush(skip: Int): Unit = {
    require(skip >= 0)
    this.skip = skip
  }

  override def push(entries: Iterable[T]): Unit = {
    entries foreach { e =>
      nextSequenceNum += (skip + 1)
      entriesMap += (nextSequenceNum -> Entry("id", new DateTime(), Content(e, ""), Nil))
    }
    sequenceNumbersToSkipForPush(0)
  }

  override def push(uuid: String, entry: T): Unit = ???

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

  override def getFeedEntries(start: Long, pageSize: Int, forward: Boolean): List[FeedEntry] = {
    if (forward)
      entriesMap.dropWhile(_._1 < start).take(pageSize).toList.map(toFeedEntry)
    else
      entriesMap.takeWhile(_._1 <= start).toList.reverse.take(pageSize).map(toFeedEntry)
  }

  override def getMostRecentFeedEntries(count: Int) = {
    entriesMap.toList.reverse.take(count).map(toFeedEntry)
  }

  override def toString: String = {
    entriesMap.toString()
  }

  private def toFeedEntry(t: (Long, Entry[T])): FeedEntry = {
    FeedEntry(t._1, t._2)
  }

}

