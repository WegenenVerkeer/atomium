package be.vlaanderen.awv.atom


trait EntryConsumer[E] {
  def consume(position:FeedPosition, entry:Entry[E]) : FeedProcessingResult
}
