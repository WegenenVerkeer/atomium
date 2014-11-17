package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom
import be.vlaanderen.awv.atom.{FeedProcessingException, FeedProcessingResult, Entry, FeedPosition}
import be.vlaanderen.awv.atom.java
import com.typesafe.scalalogging.slf4j.Logging

import scala.util.{Success, Failure}

/**
 * Wrapper around the [[be.vlaanderen.awv.atom.EntryConsumer]] that offers a Java-like interface.
 *
 * @param underlying the underlying [[be.vlaanderen.awv.atom.EntryConsumer]]
 * @tparam E the type of the entries in the feed
 */
class EntryConsumerWrapper[E](underlying: java.EntryConsumer[E]) extends atom.EntryConsumer[E] with Logging {

  override def apply(position: FeedPosition, entry: Entry[E]): FeedProcessingResult = {
    try {
      underlying.accept(position, entry)
      Success()
    } catch {
      case ex:Exception =>
        logger.error(s"Error during entry consumption [$entry]", ex)
        Failure(FeedProcessingException(Option(position), ex.getMessage))
    }
  }
}
