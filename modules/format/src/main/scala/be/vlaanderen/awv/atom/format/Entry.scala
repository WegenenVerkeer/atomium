package be.vlaanderen.awv.atom.format

case class Entry[T](content: Content[T], links: List[Link])
