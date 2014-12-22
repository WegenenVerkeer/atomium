package be.wegenenverkeer.atom.jdbc

import java.sql.ResultSet

import org.joda.time.LocalDateTime

case class EntryModel(id: Option[Long], uuid: String, value: String, timestamp: LocalDateTime)

object EntryModel {

  def apply(rs: ResultSet): EntryModel = EntryModel(
    id = Some(rs.getLong(EntryModel.Table.idColumn)),
    uuid = rs.getString(EntryModel.Table.uuidColumn),
    value = rs.getString(EntryModel.Table.valueColumn),
    timestamp = new LocalDateTime(rs.getDate(EntryModel.Table.timestampColumn))
  )

  object Table {
    val idColumn = "id"
    val uuidColumn = "uuid"
    val valueColumn = "value"
    val timestampColumn = "timestamp"
  }

}
