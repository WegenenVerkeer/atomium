package controllers

import _root_.java.util.UUID

import be.vlaanderen.awv.atom._
import org.joda.time.LocalDateTime

import scala.collection.mutable.ListBuffer

class MemoryFeedStore[T](feedName: String, baseUrl: Url, title : Option[String], contentType: String = "text/plain") extends FeedStore[T](feedName, title, new UrlBuilder {

  override def base: Url = baseUrl

  override def feedLink(start: Int, count: Int): Url = Url(feedName) / start.toString / count.toString

  override def collectionLink: Url = ???
}) {

  val entries: ListBuffer[Entry[T]] = new ListBuffer[Entry[T]]

  override def context: Context = ???

  override def maxId = entries.size

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
  override def getFeedEntries(start: Int, pageSize: Int): List[Entry[T]] = {
    entries.drop(start).take(pageSize).reverse.toList
  }

}

