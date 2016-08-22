package be.wegenenverkeer.atomium.format.pub

import java.time.OffsetDateTime

import be.wegenenverkeer.atomium.format.{Content, Entry, Link}

case class AtomPubEntry[+T](id: String,
                            updated: OffsetDateTime,
                            content: Content[T],
                            links: List[Link],
                            edited:OffsetDateTime,
                            control:Control ) extends Entry[T]