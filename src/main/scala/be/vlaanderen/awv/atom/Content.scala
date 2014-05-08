package be.vlaanderen.awv.atom

case class Content[T](value: List[T], rawType: String)