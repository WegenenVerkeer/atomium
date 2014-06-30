package be.vlaanderen.awv.atom.models

import be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple._

import scala.slick.lifted.TableQuery

class EntryTable(tag: Tag) extends Table[EntryModel](tag, "FEED_ENTRY") {

  def feedName = column[String]("feed_name")
  def value = column[String]("value")
  def feedId = column[Long]("feed_id")

  def * = (feedName, value, feedId) <> (EntryModel.tupled, EntryModel.unapply)
}

object EntryTable extends TableQuery(new EntryTable(_))
