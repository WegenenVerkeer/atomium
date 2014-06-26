package be.vlaanderen.awv.atom.javaapi;

import be.vlaanderen.awv.atom.Feed;

interface FeedProvider<E> {
    Feed<E> fetchFeed();
    Feed<E> fetchFeed(String page);
}
