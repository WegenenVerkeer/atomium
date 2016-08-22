package be.wegenenverkeer.atomium.format

import java.time.OffsetDateTime


case class AtomEntry[+T](id: String, updated: OffsetDateTime, content: Content[T], links: List[Link]) extends Entry[T]