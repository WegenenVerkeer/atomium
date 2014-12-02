package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom
import be.vlaanderen.awv.atom._
import scala.util.Try

/**
 * Wrapper around the [[be.vlaanderen.awv.atom.EntryConsumer]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[be.vlaanderen.awv.atom.EntryConsumer]]
 * @tparam E the type of the entries in the feed
 */
class EntryConsumerWrapper[E](underlying: java.EntryConsumer[E]) extends atom.EntryConsumer[E] {

  override def apply(position: FeedPosition, entry: Entry[E]): FeedProcessingResult =
    Try(underlying.accept(position, entry))

}
