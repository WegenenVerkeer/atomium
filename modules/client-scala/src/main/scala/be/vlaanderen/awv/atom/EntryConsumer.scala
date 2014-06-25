package be.vlaanderen.awv.atom


import scalaz.ValidationNel


trait EntryConsumer[E] {
  def consume(position:FeedPosition, entry:Entry[E]) : ValidationNel[String, Unit]
}
