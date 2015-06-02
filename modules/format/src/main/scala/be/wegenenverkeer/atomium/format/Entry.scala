package be.wegenenverkeer.atomium.format

import org.joda.time.DateTime

/**
 * Representation of an entry in an Atom feed. The entry acts as a container for metadata and data associated with the
 * entry.
 *
 * @tparam T the type of entry
 */
trait Entry[+T] {

  /** the id of the entry */
  def id: String

  /** the content in the entry */
  def updated: DateTime

  /** when the entry was last updated */
  def content: Content[T]

  /** links associated with this entry */
  def links: List[Link]
}

