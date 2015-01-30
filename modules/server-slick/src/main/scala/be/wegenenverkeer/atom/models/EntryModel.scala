package be.wegenenverkeer.atom.models

import org.joda.time.DateTime

case class EntryModel(
  id: Option[Long],
  uuid: String,
  value: String,
  timestamp: DateTime)
