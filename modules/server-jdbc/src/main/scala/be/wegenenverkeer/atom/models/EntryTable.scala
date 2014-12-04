package be.wegenenverkeer.atom.models

import be.wegenenverkeer.atom.slick.SlickPostgresDriver
import be.wegenenverkeer.atom.slick.SlickPostgresDriver.simple._
import org.joda.time.LocalDateTime

class EntryTable(tag: Tag, tableName: String) extends Table[EntryModel](tag, tableName) {

  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def uuid = column[String]("uuid")
  def value = column[String]("value")
  def timestamp = column[LocalDateTime]("timestamp", O.NotNull)

  def * = (id.?, uuid, value, timestamp) <>(EntryModel.tupled, EntryModel.unapply)

}
