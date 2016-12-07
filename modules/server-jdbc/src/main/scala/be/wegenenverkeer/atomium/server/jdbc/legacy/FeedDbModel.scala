package be.wegenenverkeer.atomium.server.jdbc.legacy

import java.sql.ResultSet

case class FeedDbModel(id: Option[Long], name: String, title: Option[String]) {
  def autoEntryTableName = s"FEED_ENTRIES_$name"
}

object FeedDbModel {

  def apply(rs: ResultSet): FeedDbModel = FeedDbModel(
    id = Some(rs.getLong(FeedDbModel.Table.idColumn)),
    name = rs.getString(FeedDbModel.Table.nameColumn),
    title = Option(rs.getString(FeedDbModel.Table.titleColumn))
  )

  object Table {
    val name = "FEEDS"
    val idColumn = "id"
    val nameColumn = "name"
    val titleColumn = "title"
  }

}
