package be.vlaanderen.awv.atom.models

import be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple._

class FeedInfoTable(tag: Tag) extends Table[FeedInfoModel](tag, "FEED_INFO") {
  def id = column[Option[Long]]("id", O.AutoInc, O.NotNull)
  def feedName = column[String]("feed_name")
  def lastPageId = column[Long]("last_page_id")
  def pageCount = column[Int]("page_count")

  def pk = primaryKey("pk_feed_info", (id, feedName))

  def * = (id, feedName, lastPageId, pageCount) <> (FeedInfoModel.tupled, FeedInfoModel.unapply)
}

object FeedInfoTable extends TableQuery(new FeedInfoTable(_)) {

  def findLastByFeedName(feedName: String)(implicit session: Session): Option[FeedInfoModel] = {
    val maxId = FeedInfoTable.filter(_.feedName === feedName).map(_.id).max

    val query = for {
      t <- this
      if (t.id === maxId)
    } yield t
    query.firstOption
  }
}
