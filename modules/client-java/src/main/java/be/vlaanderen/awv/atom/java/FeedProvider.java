package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Feed;
import be.vlaanderen.awv.atom.FeedProcessingError;
import fj.data.Validation;

public interface FeedProvider<T> {
    Validation<FeedProcessingError, Feed<T>> fetchFeed();
    Validation<FeedProcessingError, Feed<T>> fetchFeed(String page);

    void start();
    void stop();
}
