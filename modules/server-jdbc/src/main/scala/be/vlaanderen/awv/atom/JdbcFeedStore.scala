package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.{Link, Feed, Entry, Content}
import be.vlaanderen.awv.atom.models._
import org.joda.time.LocalDateTime
import slick.SlickPostgresDriver.simple._

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
class JdbcFeedStore[E](c: JdbcContext, feedName: String, title: Option[String], ser: E => String, deser: String => E, urlBuilder: UrlBuilder) extends FeedStore[E] {

  lazy val context = c
  val urlProvider = urlBuilder

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

  override def getHeadOfFeed(pageSize: Int): Option[Feed[E]] = {
    getHeadOfFeed(pageSize, maxId.toInt)
  }

  override def getFeed(start:Int, pageSize: Int): Option[Feed[E]] = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session]
    for {
      entries <- Some(feedModel.entriesTableQuery.sortBy(_.id).drop(start).take(pageSize).list().reverse); if entries.size > 0
    } yield Feed(
      base = urlProvider.base / feedName,
      title = feedModel.title,
      updated = entries.head.timestamp.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"),
      links = List(Link(Link.selfLink, urlProvider.feedLink(start, pageSize)),
        Link(Link.lastLink, urlProvider.feedLink(0, pageSize))) ++
        getNextLink(start, pageSize) ++
        getPreviousLink(start, pageSize, maxId.toInt),
      entries = entries map { entry =>
        Entry(Content(List(deser(entry.value)), ""), Nil)
    })
  }

  override def push(entries: Iterable[E]): Unit = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session]
    val timestamp: LocalDateTime = new LocalDateTime()
    entries foreach { entry =>
      feedModel.entriesTableQuery += EntryModel(None, ser(entry), timestamp)
    }
  }

  private def maxId: Long = {
    // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session]
    Query(feedModel.entriesTableQuery.map(_.id).max).first().getOrElse(0)
  }

}
