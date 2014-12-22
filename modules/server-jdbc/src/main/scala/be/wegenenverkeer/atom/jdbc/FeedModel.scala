package be.wegenenverkeer.atom.jdbc

import java.sql.ResultSet

case class FeedModel(id: Option[Long], name: String, title: Option[String]) {
  def autoEntryTableName = s"FEED_ENTRIES_${id.get}"
}

object FeedModel {

  def apply(rs: ResultSet): FeedModel = FeedModel(
    id = Some(rs.getLong(FeedModel.Table.idColumn)),
    name = rs.getString(FeedModel.Table.nameColumn),
    title = Option(rs.getString(FeedModel.Table.titleColumn))
  )

  object Table {
    val name = "FEEDS"
    val idColumn = "id"
    val nameColumn = "name"
    val titleColumn = "title"
  }

}
