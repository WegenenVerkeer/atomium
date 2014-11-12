package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.Entry


trait EntryConsumer[E] extends ((FeedPosition, Entry[E]) => FeedProcessingResult)
