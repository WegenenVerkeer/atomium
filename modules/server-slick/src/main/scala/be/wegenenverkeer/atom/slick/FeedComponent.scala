package be.wegenenverkeer.atom.slick

import java.sql.Date

import be.wegenenverkeer.atom.models.{EntryModel, FeedModel}
import org.joda.time.LocalDateTime

trait FeedComponent extends DriverComponent {
  this: DriverComponent =>

  import driver.simple._

  def entriesTableQuery(entriesTableName: String) = {
    TableQuery[EntryTable]((tag:Tag) => new EntryTable(tag, entriesTableName))
  }

  implicit val jodaLocalDateTimeColumnType = MappedColumnType.base[LocalDateTime, Date](
  { ldt => new Date(ldt.toDate.getTime) },    // map LocalDateTime to sql.Date
  { sd => new LocalDateTime(sd.getTime) } // map sql.Date to LocalDateTime
  )

  class EntryTable(tag: Tag, tableName: String) extends Table[EntryModel](tag, tableName) {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def uuid = column[String]("uuid")
    def value = column[String]("value", O.DBType("CLOB"))
    def timestamp = column[LocalDateTime]("timestamp", O.NotNull)

    def * = (id.?, uuid, value, timestamp) <> (EntryModel.tupled, EntryModel.unapply)

  }

  class FeedTable(tag: Tag) extends Table[FeedModel](tag, "FEEDS") {

    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def title = column[Option[String]]("title")

    def pk = index("index_feed_name", name, unique = true)

    def * = (id, name, title) <> (FeedModel.tupled, FeedModel.unapply)
  }

  object Feeds extends TableQuery(new FeedTable(_)) {

    private def queryByName(feedName:String) = for {
      f <- Feeds if f.name === feedName
    } yield f

    def findByName(feedName: String)(implicit session: Session): Option[FeedModel] = {
      queryByName(feedName).firstOption
    }

  }

}
