package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.{FeedContent, Entry}


/**
 * An entry consumer is responsible for consuming the new entries offered by the feed processor.
 *
 * The entry consumer should handle the new event and persist the current feed position (possibly in 1 database
 * transaction).
 *
 * @tparam E the type of the entries in the feed
 */
trait EntryConsumer[E] extends ((FeedPosition, Entry[E]) => FeedProcessingResult)
