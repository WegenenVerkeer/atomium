package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.format.Entry;
import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingException;

public interface EntryConsumer<E> {
    void accept(FeedPosition position, Entry<E> entry) throws FeedProcessingException;
}
