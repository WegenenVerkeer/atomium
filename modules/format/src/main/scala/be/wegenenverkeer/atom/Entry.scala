package be.wegenenverkeer.atom

import org.joda.time.DateTime

/**
 * Representation of an entry in an Atom feed. The entry acts as a container for metadata and data associated with the
 * entry.
 *
 * @param id the id of the entry
 * @param content the content in the entry
 * @param updated when the entry was last updated
 * @param links links associated with this entry
 * @tparam T the type of entry
 */
case class Entry[+T](id: String, updated: DateTime, content: Content[T], links: List[Link])
