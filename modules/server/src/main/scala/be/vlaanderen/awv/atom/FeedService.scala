package be.vlaanderen.awv.atom

class FeedService[E, C <: Context](feedName: String, entriesPerPage: Int, title: String, feedStoreFactory: (String, C) => FeedStore[E]) {

  def push(elements: Iterable[E])(implicit context: C): Unit = {
    val feedPusher = new FeedPusher[E](feedStoreFactory(feedName, context), entriesPerPage, title)
    feedPusher.push(elements)(context)
  }

  def push(element: E)(implicit context: C): Unit = {
    push(List(element))(context)
  }

  def getFeed(page:Long)(implicit context: C):Option[Feed[E]] = {
    val feedPusher = new FeedPusher[E](feedStoreFactory(feedName, context), entriesPerPage, title)
    feedPusher.getFeed(page)
  }

}
