package be.vlaanderen.awv.atom.models

import org.joda.time.LocalDateTime

case class EntryModel(
  id: Option[Long],
  value: String,
  timestamp: LocalDateTime)
