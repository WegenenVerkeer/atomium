package be.wegenenverkeer.atom

import scala.util.Try

trait FeedProvider[E] {

  def initialEntryRef: Option[EntryRef[E]]

  /**
   * Fetch the first page of the feed.
   *
   * @return the first page of the feed.
   */
  def fetchFeed(): Try[Feed[E]]

  /**
   * Fetch a specific page of the feed.
   *
   * @param pageUrl the page
   * @return a page of the feed
   */
  def fetchFeed(pageUrl: String): Try[Feed[E]]

}