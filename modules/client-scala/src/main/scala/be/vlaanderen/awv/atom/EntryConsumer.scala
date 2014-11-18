package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.{FeedContent, Entry}


trait EntryConsumer[E <: FeedContent] extends ((FeedPosition, Entry[E]) => FeedProcessingResult)
