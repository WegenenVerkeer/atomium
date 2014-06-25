package be.vlaanderen.awv.atom

case class Entry[T](content: Content[T], links: List[Link])
