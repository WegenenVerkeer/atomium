package be.wegenenverkeer.atom

import _root_.java.util.UUID

import be.wegenenverkeer.atom.models.{EntryModel, EntryTable, FeedModel, FeedTable}
import be.wegenenverkeer.atom.slick.SlickPostgresDriver
import be.wegenenverkeer.atom.slick.SlickPostgresDriver.simple._
import org.joda.time.LocalDateTime

/**
 * [[AbstractFeedStore]] implementation that stores feeds and pages in a Postgres database.
 *
 * @param c the context implementation
 * @param feedName the name of the feed
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param urlBuilder helper to build urls
 * @tparam E type of the elements in the feed
 */
class JdbcFeedStore[E](c: JdbcContext, 
                       feedName: String, 
                       title: Option[String], 
                       ser: E => String, 
                       deser: String => E, 
                       urlBuilder: UrlBuilder)
  extends AbstractFeedStore[E](feedName, title, urlBuilder) {

  override lazy val context = c

  lazy val feedModel : FeedModel = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[SlickPostgresDriver.simple.Session]
    FeedTable.findByName(feedName).getOrElse {
      val id = FeedTable returning FeedTable.map(_.id) += new FeedModel(None, feedName, title)
      val f = new FeedModel(id, feedName, title)
      f.entriesTableQuery.ddl.create
      f
    }
  }

  /**
   * Retrieves entries with their sequence numbers from the feed
   *
   * @param start the starting entry (inclusive), MUST be returned in the entries
   * @param count the number of entries to return
   * @param ascending if true return entries with sequence numbers >= start in ascending order
   *                  else return entries with sequence numbers <= start in descending order
   * @return the corresponding entries sorted accordingly
   */
  override def getFeedEntries(start:Long, count: Int, ascending: Boolean): List[(Long, Entry[E])] = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[SlickPostgresDriver.simple.Session]

    val query = if (ascending)
      feedModel.entriesTableQuery.filter(e => e.id >= start).sortBy(_.id)
    else
      feedModel.entriesTableQuery.filter(e => e.id <= start).sortBy(_.id.desc)

    query.take(count).list.map(entryWithSequenceNumber)
  }

  override def push(entries: Iterable[E]): Unit = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[SlickPostgresDriver.simple.Session]
    val timestamp: LocalDateTime = new LocalDateTime()
    entries foreach { entry =>
      feedModel.entriesTableQuery += EntryModel(None, UUID.randomUUID().toString, ser(entry), timestamp)
    }
  }

  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true): Long = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[SlickPostgresDriver.simple.Session]
    if (inclusive)
      feedModel.entriesTableQuery.filter(_.id <= sequenceNr).length.run
    else
      feedModel.entriesTableQuery.filter(_.id < sequenceNr).length.run
  }

  /**
   * retrieves the most recent entries from the feedstore sorted in descending order
   * @param count the amount of recent entries to return
   * @return a list containing tuples of a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  override def getMostRecentFeedEntries(count: Int): List[(Long, Entry[E])] = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[SlickPostgresDriver.simple.Session]

    feedModel.entriesTableQuery
      .sortBy(_.id.desc)
      .take(count)
      .list().map(entryWithSequenceNumber)
  }

  /**
   * convert a database row (dbEntry) to a tuple containing sequence number and Entry
   * @return the corresponding tuple
   */
  private[this] def entryWithSequenceNumber: (EntryTable#TableElementType) => (Long, Entry[E]) = { dbEntry =>
    (dbEntry.id.get, Entry(dbEntry.uuid, dbEntry.timestamp, Content(deser(dbEntry.value), ""), Nil))
  }

  /**
   * @return one less than the minimum sequence number used in this feed
   *         since SQL sequences start at 1 this is 0. If your DB sequences start with another number override this
   *         class and modify accordingly
   */
  override val minId: Long = 0L


  override def maxId: Long = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[SlickPostgresDriver.simple.Session]
    Query(feedModel.entriesTableQuery.map(_.id).max).first().getOrElse(minId)
  }
}
