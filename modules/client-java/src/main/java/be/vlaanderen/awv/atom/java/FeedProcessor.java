package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingException;

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

    private final be.vlaanderen.awv.atom.FeedProcessor underlying;

    /**
     *
     * @param feedProvider the feed provider is responsible for fetching the feed pages
     * @param entryConsumer the entry consumer is responsible to consume the new entries
     */
    public FeedProcessor(FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        underlying = new be.vlaanderen.awv.atom.FeedProcessor<E>(
            new FeedProviderWrapper<E>(feedProvider),
            new EntryConsumerWrapper<E>(entryConsumer)
        );
    }

    /**
     * Start the consuming of Feeds.
     *
     * @throws FeedProcessingException
     */
    public void start() throws FeedProcessingException {
        underlying.start().get();
    }

}