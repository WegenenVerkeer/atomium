package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingError;
import fj.data.Validation;

public class FeedProcessor<E> {

    private final be.vlaanderen.awv.atom.FeedProcessor underlying;

    public FeedProcessor(FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        this(null, feedProvider, entryConsumer);
    }

    public FeedProcessor(FeedPosition feedPosition, FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        underlying = new be.vlaanderen.awv.atom.FeedProcessor<E>(
            feedPosition,
            new FeedProviderWrapper<E>(feedProvider),
            new EntryConsumerWrapper<E>(entryConsumer)
        );
    }

    public Validation<FeedProcessingError, FeedPosition> start() {
        scalaz.Validation<FeedProcessingError, FeedPosition> result = underlying.start();
        return Validations.toJavaValidation(result);
    }

}