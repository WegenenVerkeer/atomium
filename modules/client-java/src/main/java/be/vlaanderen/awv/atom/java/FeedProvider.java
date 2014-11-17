package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Feed;
import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingException;

/**
 * A feed provider is responsible for providing the feed pages.
 *
 * Currently, Atomium comes with 1 implementation out-of-the-box, a provider that uses the Play WS API for fetching feed
 * pages via HTTP, [[be.vlaanderen.awv.atom.providers.PlayWsBlockingFeedProvider]].
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
    Feed<T> fetchFeed() throws FeedProcessingException;

    /**
     * Fetch a specific page of the feed.
     *
     * @param page the page
     * @return a page of the feed
     * @throws FeedProcessingException
     */
    Feed<T> fetchFeed(String page) throws FeedProcessingException;
    FeedPosition getInitialPosition();

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
