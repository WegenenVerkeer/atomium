package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.client.EntryRef;
import be.wegenenverkeer.atomium.japi.format.Feed;

/**
 * A feed provider is responsible for providing the feed pages.
 *
 * @param <E> the type of the entries in the feed
 */
public interface FeedProvider<E> {

    /**
     * Fetch the page containing the give EntryRef.
     *
     * @return the first page of the feed.
     * @throws FeedProcessingException
     */
    Feed<E> fetchFeed(EntryRef<E> entryRef) throws FeedProcessingException;

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

}
