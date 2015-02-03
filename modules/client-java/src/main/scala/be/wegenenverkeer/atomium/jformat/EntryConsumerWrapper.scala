package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom
import be.wegenenverkeer.atomium.format
import be.wegenenverkeer.atomium.format.{Entry, JFeedConverters}

import scala.util.Try

/**
 * Wrapper around the [[be.wegenenverkeer.atom.java.EntryConsumer]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[be.wegenenverkeer.atom.java.EntryConsumer]]
 * @tparam E the type of the entries in the feed
 */
class EntryConsumerWrapper[E](underlying: be.wegenenverkeer.atom.java.EntryConsumer[E])
  extends be.wegenenverkeer.atom.EntryConsumer[E] {

  override def apply(entry: format.Entry[E]): Try[Entry[E]] = {
    val consumedEntry = underlying.accept(JFeedConverters.entry2JEntry(entry))
    Try(JFeedConverters.jEntry2Entry(consumedEntry))
  }

}
