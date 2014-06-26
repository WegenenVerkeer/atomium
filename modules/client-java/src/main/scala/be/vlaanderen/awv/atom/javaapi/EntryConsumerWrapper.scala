package be.vlaanderen.awv.atom.javaapi

import be.vlaanderen.awv.atom.{Entry, FeedPosition}
import be.vlaanderen.awv.atom.javaapi.{EntryConsumer => JEntryConsumer}

import scalaz._
import Scalaz._
import scalaz.ValidationNel

class EntryConsumerWrapper[E](javaEntryConsumer: JEntryConsumer[E]) extends
  be.vlaanderen.awv.atom.EntryConsumer[E] {

  override def consume(position: FeedPosition, entry: Entry[E]): ValidationNel[String, Unit] = {
    try {
      javaEntryConsumer.consume(position, entry)
      ().successNel[String]
    } catch {
      case ex:Exception => ex.getMessage.failNel[Unit]
    }
  }
}
