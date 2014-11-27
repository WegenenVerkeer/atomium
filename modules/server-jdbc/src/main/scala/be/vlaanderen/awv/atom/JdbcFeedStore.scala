package be.vlaanderen.awv.atom

import _root_.java.util.UUID

import be.vlaanderen.awv.atom.models._
import be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple._
import org.joda.time.LocalDateTime

/**
 * [[be.vlaanderen.awv.atom.FeedStore]] implementation that stores feeds and pages in a Postgres database.
 *
 * @param c the context implementation
 * @param feedName the name of the feed
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param urlBuilder helper to build urls
 * @tparam E type of the elements in the feed
 */
class JdbcFeedStore[E](c: JdbcContext, feedName: String, title: Option[String], ser: E => String, deser: String => E, urlBuilder: UrlBuilder) extends FeedStore[E](feedName, title, urlBuilder) {

  override lazy val context = c

  lazy val feedModel : FeedModel = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session]
    FeedTable.findByName(feedName).getOrElse {
      val id = FeedTable returning FeedTable.map(_.id) += new FeedModel(None, feedName, title)
      val f = new FeedModel(id, feedName, title)
      f.entriesTableQuery.ddl.create
      f
    }
  }

  override def getFeedEntries(start:Int, pageSize: Int): List[Entry[E]] = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session]
    feedModel.entriesTableQuery.sortBy(_.id).drop(start).take(pageSize).list().reverse.map {
      entry =>
        Entry(entry.uuid, entry.timestamp, Content(deser(entry.value), ""), Nil)
    }
  }

  override def push(entries: Iterable[E]): Unit = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session]
    val timestamp: LocalDateTime = new LocalDateTime()
    entries foreach { entry =>
      feedModel.entriesTableQuery += EntryModel(None, UUID.randomUUID().toString, ser(entry), timestamp)
    }
  }

  override def maxId: Long = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session]
    Query(feedModel.entriesTableQuery.map(_.id).max).first().getOrElse(0)
  }

}
