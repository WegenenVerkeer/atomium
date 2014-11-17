package be.vlaanderen.awv.atom

/**
 * Representation of the content element in a Atom feed.
 *
 * @param value the content value
 * @param rawType
 * @tparam T the type of entry
 */
case class Content[T](value: List[T], rawType: String)