package be.wegenenverkeer.atomium.server.jdbc

import java.sql.ResultSet

import org.joda.time.DateTime

/**
 * The entry model as it is stored in the DB.
 *
 * @param sequenceNo The unique DB id, this has no business meaning to the outside world, except to the extend that it is used
 *                   to define the order of the entries.
 * @param uuid The business id for this entry.
 * @param value The actual data stored in the entry, serialized as a sring.
 * @param timestamp The time this entry was created and stored.
 */
case class EntryDbModel(sequenceNo: Option[Long], uuid: String, value: String, timestamp: DateTime)

object EntryDbModel {

  def apply(rs: ResultSet): EntryDbModel = EntryDbModel(
    sequenceNo = Some(rs.getLong(EntryDbModel.Table.idColumn)),
    uuid = rs.getString(EntryDbModel.Table.uuidColumn),
    value = rs.getString(EntryDbModel.Table.valueColumn),
    timestamp = new DateTime(rs.getDate(EntryDbModel.Table.timestampColumn))
  )

  object Table {
    val idColumn = "id"
    val uuidColumn = "uuid"
    val valueColumn = "value"
    val timestampColumn = "timestamp"
  }

}
