package be.vlaanderen.awv.atom.format

/**
 * Representation of an entry in an Atom feed. The entry acts as a container for metadata and data associated with the
 * entry.
 *
 * @param content the content in the entry
 * @param links links associated with this entry
 * @tparam T the type of entry
 */
case class Entry[T](content: Content[T], links: List[Link])
