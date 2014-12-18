package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.models.EntryModel
import be.wegenenverkeer.atom.slick.FeedComponent
import org.joda.time.LocalDateTime

/**
 * [[AbstractFeedStore]] implementation that stores feeds and pages in a SQL database.
 * This implementation automatically manages the entries tables for each feed.
 *
 * @param context: the context implementation (wraps a session)
 * @param feedName the name of the feed
 * @param title the optional title of the feed
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param urlBuilder helper to build urls
 * @tparam E type of the elements in the feed
 */
abstract class AbstractJdbcFeedStore[E](
                       context: JdbcContext,
                       feedName: String,
                       title: Option[String],
                       ser: E => String,
                       deser: String => E,
                       urlBuilder: UrlBuilder) extends AbstractFeedStore[E](feedName, title, urlBuilder) {

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
  override def getFeedEntries(start:Long, count: Int, ascending: Boolean): List[FeedEntry] = {
    implicit val session = context.session

    val query = if (ascending)
      getEntryTableQuery.filter(e => e.id >= start).sortBy(_.id)
    else
      getEntryTableQuery.filter(e => e.id <= start).sortBy(_.id.desc)

    query.take(count).list.map(toFeedEntry)
  }

  override def push(entries: Iterable[E]): Unit = {
    implicit val session = context.session
    val timestamp: LocalDateTime = new LocalDateTime()
    entries foreach { entry =>
      getEntryTableQuery += EntryModel(None, generateEntryID(), ser(entry), timestamp)
    }
  }

  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true): Long = {
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
  override def getMostRecentFeedEntries(count: Int): List[FeedEntry] = {
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
      Entry(dbEntry.uuid, dbEntry.timestamp, Content(deser(dbEntry.value), ""), Nil))
  }

  /**
   * @return one less than the minimum sequence number used in this feed
   *         since SQL sequences start at 1 this is 0. If your DB sequences start with another number override this
   *         class and modify accordingly
   */
  override val minId: Long = 0L

  override def maxId: Long = {
    implicit val session = context.session
    Query(getEntryTableQuery.map(_.id).max).first(session).getOrElse(minId)
  }
}

