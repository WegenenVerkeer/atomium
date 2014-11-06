package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Feed;
import be.vlaanderen.awv.atom.FeedProcessingException;

public interface FeedProvider<T> {
    Feed<T> fetchFeed() throws FeedProcessingException;
    Feed<T> fetchFeed(String page) throws FeedProcessingException;

    void start();
    void stop();
}
