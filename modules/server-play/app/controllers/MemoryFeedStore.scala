package controllers

import _root_.java.util.UUID

import be.wegenenverkeer.atom._
import org.joda.time.LocalDateTime

import scala.collection.mutable.ListBuffer

class MemoryFeedStore[T](feedName: String, urlBuilder: UrlBuilder, title : Option[String], contentType: String)
  extends AbstractFeedStore[T](feedName, title, urlBuilder) {


  def this(feedName: String, baseUrl: Url, title: Option[String], contentType: String = "text/plain") =
    this(feedName, MemoryFeedStore.newUrlBuilder(baseUrl, feedName), title, contentType)

  val entries: ListBuffer[Entry[T]] = new ListBuffer[Entry[T]]

  override val minId = 0L

  override def maxId = entries.size+1

  override def push(it: Iterable[T]) = {
    val timestamp: LocalDateTime = new LocalDateTime()
    it foreach { t =>
      entries append Entry(UUID.randomUUID().toString, timestamp, Content(t, ""), Nil)
    }
  }

  /**
   * @param start the start entry
   * @param pageSize the number of entries to return
   * @return pageSize entries starting from start
   */
  override def getFeedEntries(start: Long, pageSize: Int, forward: Boolean): List[(Long, Entry[T])] = {
    if (forward)
      entriesWithIndex.dropWhile(_._1 < start).take(pageSize).toList
    else
      entriesWithIndex.takeWhile(_._1 <= start).toList.reverse.take(pageSize)
  }

  def entriesWithIndex: ListBuffer[(Long, Entry[T])] = {
    entries.zipWithIndex.map { f: (Entry[T], Int) =>
      (f._2 + 1.toLong, f._1)
    }
  }

  /**
   * @return the total number of entries in the feed
   */
  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true): Long = {
    if (inclusive)
      entriesWithIndex.count(_._1 <= sequenceNr)
    else
      entriesWithIndex.count(_._1 < sequenceNr)
  }

  //select * from entries order by id desc limit L
  override def getMostRecentFeedEntries(count: Int): List[(Long, Entry[T])] = {
    entriesWithIndex.toList.reverse.take(count)
  }
}

object MemoryFeedStore {

  private def newUrlBuilder(baseUrl:Url, feedName:String) = new UrlBuilder {

    override def base: Url = baseUrl

    override def collectionLink: Url = ???
  }
}
