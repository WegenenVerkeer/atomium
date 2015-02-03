package be.wegenenverkeer.atomium.jformat;

import be.wegenenverkeer.atom.EntryRef;
import be.wegenenverkeer.atom.FeedProcessingException;

/**
 * A feed provider is responsible for providing the feed pages.
 *
 * Currently, Atomium comes with 1 implementation out-of-the-box, a provider that uses the Play WS API for fetching feed
 * pages via HTTP, [[be.wegenenverkeer.atom.providers.PlayWsBlockingFeedProvider]].
 *
 * When fetching the feed pages, a feed provider should return a [[scala.util.Try]] instead of throwing an exception.
 *
 * @param <E> the type of the entries in the feed
 */
public interface FeedProvider<E> {

    /**
     * Fetch the first page of the feed.
     *
     * @return the first page of the feed.
     * @throws FeedProcessingException
     */
    Feed<E> fetchFeed() throws FeedProcessingException;

    /**
     * Fetch a specific page of the feed.
     *
     * @param page the page
     * @return a page of the feed
     * @throws FeedProcessingException
     */
    Feed<E> fetchFeed(String page) throws FeedProcessingException;

    EntryRef<E> getInitialEntryRef();

}
