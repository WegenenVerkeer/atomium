package be.wegenenverkeer.atomium.server.slick

import java.time.OffsetDateTime
import java.util.UUID

import be.wegenenverkeer.atomium.format.{AtomEntry, Content, Entry, Url}
import be.wegenenverkeer.atomium.server.AbstractFeedStore
import be.wegenenverkeer.atomium.server.slick.models.EntryModel

/**
 * An [[AbstractFeedStore]] implementation that stores feeds and pages in a SQL database.
 * This implementation automatically manages the entries tables for each feed.
 *
 * @param feedName the name of the feed
 * @param title the optional title of the feed
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param url the base `Url`
 * @tparam E type of the elements in the feed
 */
abstract class AbstractSlickFeedStore[E](feedName: String,
                                         title: Option[String],
                                         ser: E => String,
                                         deser: String => E,
                                         url: Url)
  extends AbstractFeedStore[E, SlickJdbcContext](feedName, title, url) {

  val feedComponent: FeedComponent

  import feedComponent.driver.simple._

  protected def getEntryTableQuery: TableQuery[feedComponent.EntryTable]

  /**
   * Retrieves entries with their sequence numbers from the feed
   *
   * @param start the starting entry (inclusive), MUST be returned in the entries
   * @param count the number of entries to return
   * @param ascending if true return entries with sequence numbers >= start in ascending order
   *                  else return entries with sequence numbers <= start in descending order
   * @return the corresponding entries sorted accordingly
   */
  override def getFeedEntries(start: Long, count: Int, ascending: Boolean)(implicit context: SlickJdbcContext): List[FeedEntry] = {
    implicit val session = context.session

    val query = if (ascending)
      getEntryTableQuery.filter(e => e.id >= start).sortBy(_.id)
    else
      getEntryTableQuery.filter(e => e.id <= start).sortBy(_.id.desc)

    query.take(count).list.map(toFeedEntry)
  }

  override def push(entries: Iterable[E])(implicit context: SlickJdbcContext): Unit = {
    implicit val session = context.session
    val timestamp = OffsetDateTime.now()
    entries foreach { entry =>
      getEntryTableQuery += EntryModel(None, UUID.randomUUID().toString, ser(entry), timestamp)
    }
  }

  override def push(uuid: String, entry: E)(implicit context: SlickJdbcContext): Unit = {
    implicit val session = context.session
    val timestamp = OffsetDateTime.now()
    getEntryTableQuery += EntryModel(None, uuid, ser(entry), timestamp)
  }

  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true)(implicit context: SlickJdbcContext): Long = {
    implicit val session = context.session
    if (inclusive)
      getEntryTableQuery.filter(_.id <= sequenceNr).length.run
    else
      getEntryTableQuery.filter(_.id < sequenceNr).length.run
  }

  /**
   * retrieves the most recent entries from the feedstore sorted in descending order
   * @param count the amount of recent entries to return
   * @return a list containing tuples of a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  override def getMostRecentFeedEntries(count: Int)(implicit context: SlickJdbcContext): List[FeedEntry] = {
    implicit val session = context.session

    getEntryTableQuery
    .sortBy(_.id.desc)
    .take(count)
    .list(session).map(toFeedEntry)
  }

  /**
   * convert a database row (dbEntry) to a FeedEntry containing sequence number and Entry
   * @return the corresponding FeedEntry
   */
  private[this] def toFeedEntry: (feedComponent.EntryTable#TableElementType) => FeedEntry = { dbEntry =>
    FeedEntry(dbEntry.id.get,
      new AtomEntry[E](dbEntry.uuid, dbEntry.timestamp, new Content(deser(dbEntry.value), ""))
    )
  }

  /**
   * @return one less than the minimum sequence number used in this feed
   *         since SQL sequences start at 1 this is 0. If your DB sequences start with another number override this
   *         class and modify accordingly
   */
  override def minId(implicit context: SlickJdbcContext): Long = 0L

  override def maxId(implicit context: SlickJdbcContext): Long = {
    implicit val session = context.session
    Query(getEntryTableQuery.map(_.id).max).first(session).getOrElse(minId)
  }
}

