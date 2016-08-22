package be.wegenenverkeer.atomium.server.slick.models

import java.time.OffsetDateTime

case class EntryModel(
  id: Option[Long],
  uuid: String,
  value: String,
  timestamp: OffsetDateTime)
