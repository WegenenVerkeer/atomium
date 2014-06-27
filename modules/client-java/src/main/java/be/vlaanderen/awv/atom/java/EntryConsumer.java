package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Entry;
import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingError;
import fj.data.Validation;

interface EntryConsumer<E> {
    Validation<FeedProcessingError, FeedPosition> consume(FeedPosition position, Entry<E> entry);
}
