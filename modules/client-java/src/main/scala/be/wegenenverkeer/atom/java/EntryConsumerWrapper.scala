package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.{JFeedConverters, FeedPosition, FeedProcessingResult}

import scala.util.Try

/**
 * Wrapper around the [[be.wegenenverkeer.atom.java.EntryConsumer]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[be.wegenenverkeer.atom.java.EntryConsumer]]
 * @tparam E the type of the entries in the feed
 */
class EntryConsumerWrapper[E](underlying: be.wegenenverkeer.atom.java.EntryConsumer[E])
  extends be.wegenenverkeer.atom.EntryConsumer[E] {

  override def apply(position: FeedPosition, entry: be.wegenenverkeer.atom.Entry[E]): FeedProcessingResult =
    Try(underlying.accept(position, JFeedConverters.entry2JEntry(entry)))

}
