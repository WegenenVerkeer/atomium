package be.wegenenverkeer.atom.models

import org.joda.time.LocalDateTime

case class EntryModel(
  id: Option[Long],
  uuid: String,
  value: String,
  timestamp: LocalDateTime)
