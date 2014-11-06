package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{FeedProcessingException, FeedProcessingResult, Entry, FeedPosition}
import be.vlaanderen.awv.atom.java.{EntryConsumer => JEntryConsumer}
import com.typesafe.scalalogging.slf4j.Logging

import scala.util.{Success, Failure}

class EntryConsumerWrapper[E](underlying: JEntryConsumer[E]) extends
  be.vlaanderen.awv.atom.EntryConsumer[E] with Logging {

  override def consume(position: FeedPosition, entry: Entry[E]): FeedProcessingResult = {
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
