package be.wegenenverkeer.atom.java;

import be.wegenenverkeer.atom.FeedPosition;
import be.wegenenverkeer.atom.FeedProcessingException;

public interface EntryConsumer<E> {
    Entry<E> accept(Entry<E> entry) throws FeedProcessingException;
}
