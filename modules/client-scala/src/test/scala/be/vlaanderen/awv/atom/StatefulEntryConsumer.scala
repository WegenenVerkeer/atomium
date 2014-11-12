package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.Entry

import scala.collection.mutable
import scala.util.Success

class StatefulEntryConsumer extends EntryConsumer[String] {
  var finalPosition:Option[FeedPosition] = None
  var consumedEvents = new mutable.ListBuffer[String]

  override def apply(position: FeedPosition, eventEntry: Entry[String]): FeedProcessingResult = {
    finalPosition = Option(position)
    eventEntry.content.value.foreach { e =>
      consumedEvents += e
    }
    Success()
  }
}


