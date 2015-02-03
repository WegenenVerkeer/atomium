package be.wegenenverkeer.atomium.jformat;

import be.wegenenverkeer.atom.FeedProcessingException;

public interface EntryConsumer<E> {
    Entry<E> accept(Entry<E> entry) throws FeedProcessingException;
}
