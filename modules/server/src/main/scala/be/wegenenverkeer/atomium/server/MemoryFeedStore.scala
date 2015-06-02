package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.format.{AtomEntry, Content, Entry, Url}
import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer

/**
 * An in memory feedstore. This implementation is very inefficient and should only be used for demo or test purposes
 *
 * @param feedName the name of the feed
 * @param url the base `Url`
 * @param title the optional title of the feed
 * @param contentType the content type of the entries in the feed
 * @tparam T the type for the content of the generated feed
 */
class MemoryFeedStore[T, C <: Context](feedName: String,
                                       url: Url,
                                       title: Option[String],
                                       contentType: String) extends AbstractFeedStore[T, C](feedName, title, url) {


  private val entries: ListBuffer[Entry[T]] = new ListBuffer[Entry[T]]

  override def minId(implicit context: C) = 0L

  override def maxId(implicit context: C) = entries.size + 1

  override def push(it: Iterable[T])(implicit context: C) = {
    val timestamp: DateTime = new DateTime()
    it foreach { entry =>
      push(generateEntryID(), entry, new DateTime())
    }
  }

  override def push(uuid: String, entry: T)(implicit context: C): Unit = {
    push(uuid, entry, new DateTime())
  }

  private def push(uuid: String, entry: T, timestamp: DateTime): Unit = {
    entries append AtomEntry(uuid, timestamp, Content(entry, ""), Nil)
  }


  /**
   * @param start the start entry
   * @param pageSize the number of entries to return
   * @return pageSize entries starting from start
   */
  override def getFeedEntries(start: Long, pageSize: Int, forward: Boolean)(implicit context: C): List[FeedEntry] = {
    if (forward)
      entriesWithIndex.dropWhile(_.sequenceNr < start).take(pageSize).toList
    else
      entriesWithIndex.takeWhile(_.sequenceNr <= start).toList.reverse.take(pageSize)
  }

  def entriesWithIndex: ListBuffer[FeedEntry] = {
    entries.zipWithIndex.map { case (entry, index) =>
      FeedEntry(index + 1.toLong, entry)
    }
  }

  /**
   * @return the total number of entries in the feed
   */
  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true)(implicit context: C): Long = {
    if (inclusive)
      entriesWithIndex.count(_.sequenceNr <= sequenceNr)
    else
      entriesWithIndex.count(_.sequenceNr < sequenceNr)
  }

  //select * from entries order by id desc limit L
  override def getMostRecentFeedEntries(count: Int)(implicit context: C): List[FeedEntry] = {
    entriesWithIndex.toList.reverse.take(count)
  }

  def clear(): Unit = entries.clear()

  def count: Int = entries.size


}

