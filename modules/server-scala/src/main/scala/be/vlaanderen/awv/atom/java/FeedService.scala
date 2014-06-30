package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.Context
import scala.collection.JavaConverters._

class FeedService[E, C <: Context](context: C, feedName: String, entriesPerPage: Integer, title: String) {
  val underlying: be.vlaanderen.awv.atom.FeedService[E, C] = null

  def push(elements: java.lang.Iterable[E]) = underlying.push(elements.asScala)(context)
  def push(element: E) = underlying.push(element)(context)
}
