package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{FeedProcessingError, FeedProcessingResult, Entry, FeedPosition}
import be.vlaanderen.awv.atom.java.{EntryConsumer => JEntryConsumer}
import com.typesafe.scalalogging.slf4j.Logging

import scalaz._
import Scalaz._
import scalaz.ValidationNel

class EntryConsumerWrapper[E](underlying: JEntryConsumer[E]) extends
  be.vlaanderen.awv.atom.EntryConsumer[E] with Logging {

  override def consume(position: FeedPosition, entry: Entry[E]): FeedProcessingResult = {
    try {
      underlying.consume(position, entry)
      position.success[FeedProcessingError]
    } catch {
      case ex:Exception =>
        logger.error(s"Error during entry consumption [$entry]", ex)
        FeedProcessingError(
          Option(position),
          ex.getMessage
        ).fail[FeedPosition]
    }
  }
}
