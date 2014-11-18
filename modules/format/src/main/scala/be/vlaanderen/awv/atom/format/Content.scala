package be.vlaanderen.awv.atom.format

case class Content[T <: FeedContent](value: T, `type`: String)
