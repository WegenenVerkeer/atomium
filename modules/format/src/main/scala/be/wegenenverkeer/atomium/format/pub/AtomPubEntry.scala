package be.wegenenverkeer.atomium.format.pub

import be.wegenenverkeer.atomium.format.{Entry, Link, Content}
import org.joda.time.DateTime

case class AtomPubEntry[+T](id: String,
                            updated: DateTime,
                            content: Content[T],
                            links: List[Link],
                            edited:DateTime,
                            control:Control ) extends Entry[T]