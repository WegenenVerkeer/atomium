package be.wegenenverkeer.atom.java;

import be.wegenenverkeer.atom.AtomResult;

/**
 * A feed processor fetches pages from the feed and offers the new items to the entry consumer.
 *
 * The processor decides which feed page to fetch and which items are considered as new items based on the initial feed
 * position.
 *
 * The processor uses the feed provider to fetch the feed pages.
 *
 * @param <E> the type of the entries in the feed
 */
public class FeedProcessor<E> {

    private final be.wegenenverkeer.atom.FeedProcessor underlying;

    /**
     *
     * @param feedProvider the feed provider is responsible for fetching the feed pages
     * @param entryConsumer the entry consumer is responsible to consume the new entries
     */
    public FeedProcessor(FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        underlying = new be.wegenenverkeer.atom.FeedProcessor<E>(
            new FeedProviderWrapper<E>(feedProvider),
            new EntryConsumerWrapper<E>(entryConsumer)
        );
    }

    /**
     * Start the consuming of Feeds.
     *
     * <ul>
     *     <li>If feed is empty, returns a {@link be.wegenenverkeer.atom.AtomNothing}</li>
     *     <li>If feed is consumed, returns a {@link be.wegenenverkeer.atom.AtomSuccess} containing the last successful
     *     consumed entry</li>
     *     <li>If an error occurs, returns a {@link be.wegenenverkeer.atom.AtomFailure} containing the last successful
     *     consumed entry and the exception thrown during processing.</li>
     * </ul>
     *
     * @return AtomResult<E>
     */
    public AtomResult<E> start() {
        return underlying.start();
    }

}