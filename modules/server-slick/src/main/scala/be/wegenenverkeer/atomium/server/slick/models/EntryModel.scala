package be.wegenenverkeer.atomium.server.slick.models

import org.joda.time.DateTime

case class EntryModel(
  id: Option[Long],
  uuid: String,
  value: String,
  timestamp: DateTime)
