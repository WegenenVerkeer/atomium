package be.wegenenverkeer.atom

import scala.collection.mutable
import scala.util.{Try, Success}

class StatefulEntryConsumer extends EntryConsumer[String] {
  var lastConsumedEntryId:Option[String] = None
  var consumedEvents = new mutable.ListBuffer[String]

  override def apply(eventEntry: Entry[String]): Try[Entry[String]] = {
    lastConsumedEntryId = Option(eventEntry.id)
    consumedEvents += eventEntry.content.value
    Success(eventEntry)
  }
}


