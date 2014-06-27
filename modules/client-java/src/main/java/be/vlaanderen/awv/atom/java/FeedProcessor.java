package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingError;
import fj.data.Validation;

class FeedProcessor<E> {

    private final be.vlaanderen.awv.atom.FeedProcessor feedProcessorScala;

    FeedProcessor(FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        this(null, feedProvider, entryConsumer);
    }

    FeedProcessor(FeedPosition feedPosition, FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        feedProcessorScala = new be.vlaanderen.awv.atom.FeedProcessor<E>(
            feedPosition,
            new FeedProviderWrapper<E>(feedProvider),
            new EntryConsumerWrapper<E>(entryConsumer)
        );
    }

    public Validation<FeedProcessingError, FeedPosition> start() {
        scalaz.Validation<FeedProcessingError, FeedPosition> result = feedProcessorScala.start();
        return Validations.toJavaValidation(result);
    }

}