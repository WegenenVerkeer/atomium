package be.wegenenverkeer.atom.async

import be.wegenenverkeer.atom.EntryRef
import be.wegenenverkeer.atomium.format.Feed

import scala.concurrent.Future

trait AsyncFeedProvider[E] {

    def initialEntryRef: Option[EntryRef[E]]

    /**
     * Fetch the first page of the feed.
     *
     * @return the first page of the feed.
     */
    def fetchFeed(): Future[Feed[E]]

    /**
     * Fetch a specific page of the feed.
     *
     * @param pageUrl the page
     * @return a page of the feed
     */
    def fetchFeed(pageUrl: String): Future[Feed[E]]
}
