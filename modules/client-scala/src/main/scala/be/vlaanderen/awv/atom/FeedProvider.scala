package be.vlaanderen.awv.atom


import scalaz._

trait FeedProvider[T] {
  def fetchFeed() : Validation[FeedProcessingError, Feed[T]]
  def fetchFeed(page:String) : Validation[FeedProcessingError, Feed[T]]
}
