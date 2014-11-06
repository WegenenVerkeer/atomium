package be.vlaanderen.awv.atom.models

import be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple._

class FeedTable(tag: Tag) extends Table[FeedModel](tag, "FEED") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def title = column[Option[String]]("title")

  def pk = index("index_feed_name", name, true)

  def * = (id, name, title) <> (FeedModel.tupled, FeedModel.unapply)
}

object FeedTable extends TableQuery(new FeedTable(_)) {

  private def queryByName(feedName:String) = for {
    f <- FeedTable if f.name === feedName
  } yield f

  def findByName(feedName: String)(implicit session: Session): Option[FeedModel] = {
    queryByName(feedName).firstOption
  }

}
