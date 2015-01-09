package be.wegenenverkeer.atom.async

import be.wegenenverkeer.atom.{EntryRef, Feed}

import scala.concurrent.Future

trait AsyncFeedProvider[T] {

    def initialEntryRef: Option[EntryRef]

    /**
     * Fetch the first page of the feed.
     *
     * @return the first page of the feed.
     */
    def fetchFeed(): Future[Feed[T]]

    /**
     * Fetch a specific page of the feed.
     *
     * @param page the page
     * @return a page of the feed
     */
    def fetchFeed(page: String): Future[Feed[T]]
}
