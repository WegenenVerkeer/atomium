package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.jdbc.{EntryData, Dialect}

abstract class AbstractJdbcFeedStore[E](context: JdbcContext,
                                        feedName: String,
                                        title: Option[String],
                                        ser: E => String,
                                        deser: String => E,
                                        urlBuilder: UrlBuilder) extends AbstractFeedStore[E](feedName, title, urlBuilder) {

  // The concrete implementation of the feedstore must extend a specific SQL dialect.
  self: Dialect =>

  // The JDBC context is made implicit.
  implicit val ctx: JdbcContext = context

  // The table name for the feed entries, which has to be specified by subclasses.
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
    val entries: List[EntryData] = feedEntries(entryTableName, start, count, ascending)
    entries.map(toFeedEntry)
  }

  /**
   * retrieves the most recent entries from the feedstore sorted in descending order
   * @param count the amount of recent entries to return
   * @return a list of FeedEntries. a FeedEntry is a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  override def getMostRecentFeedEntries(count: Int): List[FeedEntry] = ???

  /**
   * @return the maximum sequence number used in this feed or minId if feed is empty
   */
  override def maxId: Long = ???

  /**
   * @return one less than the minimum sequence number used in this feed
   */
  override def minId: Long = ???

  /**
   * @param sequenceNr sequence number to match
   * @param inclusive if true include the specified sequence number
   * @return the number of entries in the feed with sequence number lower than specified
   */
  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean): Long = ???

  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  override def push(entries: Iterable[E]): Unit = ???

  /**
   * convert a database row (dbEntry) to a FeedEntry containing sequence number and Entry
   * @return the corresponding FeedEntry
   */
  private[this] def toFeedEntry: (EntryData) => FeedEntry = { entry =>
    FeedEntry(entry.id,
      Entry(entry.uuid, entry.timestamp, Content(deser(entry.value), ""), Nil))
  }

}
