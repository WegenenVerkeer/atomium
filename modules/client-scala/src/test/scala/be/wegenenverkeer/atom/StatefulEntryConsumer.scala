package be.wegenenverkeer.atom

import scala.collection.mutable
import scala.util.Success

class StatefulEntryConsumer extends EntryConsumer[String] {
  var finalPosition:Option[FeedPosition] = None
  var consumedEvents = new mutable.ListBuffer[String]

  override def apply(position: FeedPosition, eventEntry: Entry[String]): FeedProcessingResult = {
    finalPosition = Option(position)
    consumedEvents += eventEntry.content.value
    Success()
  }
}


