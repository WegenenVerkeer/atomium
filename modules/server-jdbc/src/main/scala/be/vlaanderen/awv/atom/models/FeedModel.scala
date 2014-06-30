package be.vlaanderen.awv.atom.models

import org.joda.time.LocalDateTime

case class FeedModel(
  id: Long,
  name: String,
  title: Option[String],
  timestamp: LocalDateTime,
  first: Long,
  previous: Option[Long],
  next: Option[Long]
)
