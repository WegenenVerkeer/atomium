package be.vlaanderen.awv.atom.models

import org.joda.time.LocalDateTime

case class EntryModel(
  id: Long,
  value: String,
  timestamp: LocalDateTime)
