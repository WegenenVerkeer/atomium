package be.vlaanderen.awv.atom


import scalaz._

trait FeedProvider[T] {
  def fetchFeed() : ValidationNel[String, Feed[T]]
  def fetchFeed(page:String) : ValidationNel[String, Feed[T]]
}
