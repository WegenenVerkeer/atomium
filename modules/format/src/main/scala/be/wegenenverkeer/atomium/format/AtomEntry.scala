package be.wegenenverkeer.atomium.format

import org.joda.time.DateTime

case class AtomEntry[+T](id: String, updated: DateTime, content: Content[T], links: List[Link]) extends Entry[T]