package be.vlaanderen.awv.atom.javaapi;

import be.vlaanderen.awv.atom.Entry;
import be.vlaanderen.awv.atom.FeedPosition;

interface EntryConsumer<E> {
   void consume(FeedPosition position, Entry<E> entry);
}
