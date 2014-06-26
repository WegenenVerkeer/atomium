package be.vlaanderen.awv.atom


import scalaz._

trait FeedProvider[T] {
  def fetchFeed() : Validation[String, Feed[T]]
  def fetchFeed(page:String) : Validation[String, Feed[T]]
}
