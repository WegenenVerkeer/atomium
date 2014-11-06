package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Entry;
import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingException;

public interface EntryConsumer<E> {
    void consume(FeedPosition position, Entry<E> entry) throws FeedProcessingException;
}
