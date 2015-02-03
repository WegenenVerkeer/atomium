package be.wegenenverkeer.atom

import be.wegenenverkeer.atomium.format.Entry

import scala.util.Try

/**
 * An entry consumer is responsible for consuming the new entries offered by the feed processor.
 *
 * The entry consumer should handle the new event and persist the current feed position (possibly in 1 database
 * transaction).
 *
 * @tparam E the type of the entries in the feed
 */
@deprecated
trait EntryConsumer[E] extends ((Entry[E]) => Try[Entry[E]])
