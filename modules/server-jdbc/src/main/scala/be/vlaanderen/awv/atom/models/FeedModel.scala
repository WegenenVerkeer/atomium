package be.vlaanderen.awv.atom.models

import scala.slick.lifted.{TableQuery, Tag}

case class FeedModel(
  id: Option[Long],
  name: String,
  title: Option[String]) {

  def entriesTableName = {
    s"FEED_ENTRIES_${id.get}"
  }

  def entriesTableQuery = {
    TableQuery[EntryTable]((tag:Tag) => new EntryTable(tag, entriesTableName))
  }

}
