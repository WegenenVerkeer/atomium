package be.wegenenverkeer.atom

import scala.collection.mutable
import scala.util.Success

class StatefulEntryConsumer extends EntryConsumer[String] {
  var lastConsumedEntryId:Option[String] = None
  var consumedEvents = new mutable.ListBuffer[String]

  override def apply(eventEntry: Entry[String]): FeedProcessingResult[String] = {
    lastConsumedEntryId = Option(eventEntry.id)
    consumedEvents += eventEntry.content.value
    Success(eventEntry)
  }
}


