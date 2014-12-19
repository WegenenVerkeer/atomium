package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.jdbc.{EntryData, Dialect}
import org.joda.time.LocalDateTime

abstract class AbstractJdbcFeedStore[E](context: JdbcContext,
                                        feedName: String,
                                        title: Option[String],
                                        ser: E => String,
                                        deser: String => E,
                                        urlBuilder: UrlBuilder) extends AbstractFeedStore[E](feedName, title, urlBuilder) {

  /**
   * The concrete implementation of the JDBC feed store must extend a specific SQL dialect.
   */
  dialect: Dialect =>

  /**
   * The JDBC context is made implicit.
   */
  implicit val ctx: JdbcContext = context

  /**
   * The table name for the feed entries, which has to be specified by subclasses.
   */
  def entryTableName: String

  /**
   * Retrieves entries with their sequence numbers from the feed
   *
   * @param start the starting entry (inclusive), MUST be returned in the entries
   * @param count the number of entries to return
   * @param ascending if true return entries with sequence numbers >= start in ascending order
   *                  else return entries with sequence numbers <= start in descending order
   * @return the corresponding entries sorted accordingly
   */
  override def getFeedEntries(start: Long, count: Int, ascending: Boolean): List[FeedEntry] = {
    val entries: List[EntryData] = dialect.fetchFeedEntries(entryTableName, start, count, ascending)
    entries.map(toFeedEntry)
  }

  /**
   * Retrieves the most recent entries from the feedstore sorted in descending order
   *
   * @param count the amount of recent entries to return
   * @return a list of FeedEntries. a FeedEntry is a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  override def getMostRecentFeedEntries(count: Int): List[FeedEntry] = {
    val entries: List[EntryData] = dialect.fetchMostRecentFeedEntries(entryTableName, count)
    entries.map(toFeedEntry)
  }

  /**
   * @return The maximum sequence number used in this feed or minId if feed is empty.
   */
  override def maxId: Long = {
    dialect.fetchMaxEntryId(entryTableName)
  }

  override def minId: Long = 0L

  /**
   * @param sequenceNr sequence number to match
   * @param inclusive if true include the specified sequence number
   * @return the number of entries in the feed with sequence number lower than specified
   */
  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean): Long = {
    dialect.fetchEntryCountLowerThan(entryTableName, sequenceNr, inclusive)
  }

  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  override def push(entries: Iterable[E]): Unit = {
    val timestamp: LocalDateTime = new LocalDateTime()
    entries foreach { entry =>
      dialect.addFeedEntry(entryTableName, EntryData(id = None, generateEntryID, value = ser(entry), timestamp = timestamp))
    }
  }

  def createTables() = {
    createFeedTable
    createEntryTable(entryTableName = entryTableName)
  }

  def dropTables() = {
    dropFeedTable
    dropEntryTable(entryTableName = entryTableName)
  }

  /**
   * convert a database row (dbEntry) to a FeedEntry containing sequence number and Entry
   * @return the corresponding FeedEntry
   */
  private[this] def toFeedEntry(entry: EntryData): FeedEntry = {
    FeedEntry(
      entry.id.get,
      Entry(entry.uuid, entry.timestamp, Content(deser(entry.value), ""), Nil)
    )
  }

}
