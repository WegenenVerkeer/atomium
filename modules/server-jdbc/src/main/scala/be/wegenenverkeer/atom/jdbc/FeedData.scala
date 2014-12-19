package be.wegenenverkeer.atom.jdbc

case class FeedData(id: Long, name: String, title: Option[String]) {
  def autoEntryTableName = s"FEED_ENTRIES_$id"
}

object FeedData {

//  def apply(rs: ResultSet) = FeedData(...)

  object Table {
    val name = "FEEDS"
    val idColumn = "id"
    val nameColumn = "name"
    val titleColumn = "title"
  }

}
