package be.wegenenverkeer.atomium.server.slick.models

case class FeedModel(
  id: Option[Long],
  name: String,
  title: Option[String]) {

  def entriesTableName = {
    s"FEED_ENTRIES_${id.get}"
  }

}
