package be.wegenenverkeer.atomium.client.async

import be.wegenenverkeer.atomium.client.EntryRef
import be.wegenenverkeer.atomium.format.Feed

import scala.concurrent.Future

trait AsyncFeedProvider[E] {

  // tag::adoc[]
  /**
   * Fetch the first page of the feed.
   *
   * @return the first page of the feed.
   */
  def fetchFeed(initialEntryRef: Option[EntryRef[E]] = None): Future[Feed[E]]

  /**
   * Fetch a specific page of the feed.
   *
   * @param pageUrl the page
   * @return a page of the feed
   */
  def fetchFeed(pageUrl: String): Future[Feed[E]]
  // end::adoc[]
}
