package be.wegenenverkeer.atomium.client

import java.time.LocalDateTime

case class AtomiumFeedClientStatus(ok: Boolean, status: String, error: Option[String] = None,
                                   feedPosition: Option[String] = None, entryId: Option[String] = None, receivedOn: Option[LocalDateTime] = None)


