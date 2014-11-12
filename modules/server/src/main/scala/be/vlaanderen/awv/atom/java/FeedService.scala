package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.Context
import scala.collection.JavaConverters._

class FeedService[E, C <: Context](context: C, feedName: String, entriesPerPage: Integer, title: String, feedStoreFactory: FeedStoreFactory[E, C]) {
  private val underlying: be.vlaanderen.awv.atom.FeedService[E, C] =
    new be.vlaanderen.awv.atom.FeedService[E, C](feedName, entriesPerPage, (name, context) => feedStoreFactory.create(name, context))

  def push(elements: java.lang.Iterable[E]) = underlying.push(elements.asScala)(context)

  def push(element: E) = underlying.push(element)(context)

  def getFeed(start:Int, count:Int) = underlying.getFeedPage(start, count)(context)
}
