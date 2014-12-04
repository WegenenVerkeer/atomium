package be.wegenenverkeer.atom

/**
 * Representation of the content element in a Atom feed.
 *
 * @param value the content value
 * @param `type` the content type
 * @tparam T the type of entry
 */
case class Content[T](value: T, `type`: String)
