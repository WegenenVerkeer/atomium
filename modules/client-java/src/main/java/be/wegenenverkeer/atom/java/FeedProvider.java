package be.wegenenverkeer.atom.java;

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
 * @param <T> the type of the entries in the feed
 */
public interface FeedProvider<T> {

    /**
     * Fetch the first page of the feed.
     *
     * @return the first page of the feed.
     * @throws FeedProcessingException
     */
    be.wegenenverkeer.atom.java.Feed<T> fetchFeed() throws FeedProcessingException;

    /**
     * Fetch a specific page of the feed.
     *
     * @param page the page
     * @return a page of the feed
     * @throws FeedProcessingException
     */
    be.wegenenverkeer.atom.java.Feed<T> fetchFeed(String page) throws FeedProcessingException;

    EntryRef getInitialEntryRef();

    /**
     * This method is called when the feed processor is started.
     *
     * Implementations of this method can include any setup logic here.
     */
    void start();

    /**
     * This method is called when the feed processor is stopped.
     *
     * Implementations of this method can include any cleanup logic here.
     */
    void stop();
}
