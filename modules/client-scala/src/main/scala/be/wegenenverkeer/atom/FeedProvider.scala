package be.wegenenverkeer.atom

import scala.util.Try

trait FeedProvider[T] {

  def initialEntryRef: Option[EntryRef]

  /**
   * Fetch the first page of the feed.
   *
   * @return the first page of the feed.
   */
  def fetchFeed(): Try[Feed[T]]

  /**
   * Fetch a specific page of the feed.
   *
   * @param pageUrl the page
   * @return a page of the feed
   */
  def fetchFeed(pageUrl: String): Try[Feed[T]]

}