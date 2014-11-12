package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.format.Feed;
import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingException;

public interface FeedProvider<T> {
    Feed<T> fetchFeed() throws FeedProcessingException;
    Feed<T> fetchFeed(String page) throws FeedProcessingException;
    FeedPosition getInitialPosition();

    void start();
    void stop();
}
