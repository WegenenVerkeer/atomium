package be.vlaanderen.awv.atom


trait EntryConsumer[E] extends ((FeedPosition, Entry[E]) => FeedProcessingResult)
