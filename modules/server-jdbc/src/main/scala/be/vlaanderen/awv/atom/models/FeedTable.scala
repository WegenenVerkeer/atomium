package be.vlaanderen.awv.atom.models

import org.joda.time.LocalDateTime

import be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple._

class FeedTable(tag: Tag) extends Table[FeedModel](tag, "FEED") {
  def id = column[Long]("id", O.NotNull)
  def name = column[String]("name")
  def title = column[Option[String]]("title")
  def timestamp = column[LocalDateTime]("timestamp", O.NotNull)
  def first = column[Long]("first")
  def previous = column[Option[Long]]("previous")
  def next = column[Option[Long]]("next")

  def pk = primaryKey("pk_feed", (id, name))

  def * = (id, name, title, timestamp, first, previous, next) <> (FeedModel.tupled, FeedModel.unapply)
}

object FeedTable extends TableQuery(new FeedTable(_)) {

  private def queryByType(eventType:String) = for {
    f <- FeedTable if f.name === eventType
  } yield f

  private def queryById(id: Long, eventType: String) =
    for {
      f <- queryByType(eventType) if f.id === id
    } yield f

  def findById(id: Long, eventType: String)(implicit session: Session): Option[FeedModel] = {
    queryById(id, eventType).firstOption
  }

  def findLast(eventType: String)(implicit session: Session): Option[FeedModel] = {
    (
      for {
        feed <- queryByType(eventType) if feed.next.isNull
      } yield feed
      ).firstOption
  }

  def updateFeed(feed: FeedModel)(implicit session: Session): Int = {
    queryById(feed.id, feed.name).update(feed)
  }
}
