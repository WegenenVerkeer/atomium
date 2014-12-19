package be.wegenenverkeer.atom.jdbc

import java.sql.ResultSet

import org.joda.time.LocalDateTime

case class EntryData(id: Option[Long], uuid: String, value: String, timestamp: LocalDateTime)

object EntryData {

  def apply(rs: ResultSet): EntryData = EntryData(
    id = Some(rs.getLong(EntryData.Table.idColumn)),
    uuid = rs.getString(EntryData.Table.uuidColumn),
    value = rs.getString(EntryData.Table.valueColumn),
    timestamp = new LocalDateTime(rs.getDate(EntryData.Table.timestampColumn))
  )

  object Table {
    val idColumn = "id"
    val uuidColumn = "uuid"
    val valueColumn = "value"
    val timestampColumn = "timestamp"
  }

}
