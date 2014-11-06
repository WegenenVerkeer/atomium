package be.vlaanderen.awv.atom

class FeedService[E, C <: Context](feedName: String, entriesPerPage: Int, title: String, feedStoreFactory: (String, C) => FeedStore[E]) {

  def push(elements: Iterable[E])(implicit context: C): Unit = {
    feedStoreFactory(feedName, context).push(elements)
  }

  def push(element: E)(implicit context: C): Unit = {
    push(List(element))(context)
  }

  def getFeedPage(start: Int, count:Int)(implicit context: C):Option[Feed[E]] = {
    if (count == entriesPerPage /*&& start % count == 0*/) { // TODO start parameter should be ok
      feedStoreFactory(feedName, context).getFeed(start, count)
    } else {
      None
    }
  }
  
  def getHeadOfFeed()(implicit context: C) : Option[Feed[E]] = {
    feedStoreFactory(feedName, context).getHeadOfFeed(entriesPerPage)
  }

}
