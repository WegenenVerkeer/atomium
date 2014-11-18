package be.vlaanderen.awv.atom.format

case class Entry[T <: FeedContent](content: Content[T], links: List[Link])


