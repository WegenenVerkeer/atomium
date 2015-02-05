package be.wegenenverkeer.atomium.client

import be.wegenenverkeer.atomium.format.Feed

import scala.util.Try

trait FeedProvider[E] {

  /**
   * Fetch the first page of the feed.
   *
   * @return the first page of the feed.
   */
  def fetchFeed(initialEntryRef: Option[EntryRef[E]] = None): Try[Feed[E]]

  /**
   * Fetch a specific page of the feed.
   *
   * @param pageUrl the page
   * @return a page of the feed
   */
  def fetchFeed(pageUrl: String): Try[Feed[E]]

}