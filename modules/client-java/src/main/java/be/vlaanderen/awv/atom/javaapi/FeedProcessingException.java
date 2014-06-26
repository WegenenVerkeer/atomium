package be.vlaanderen.awv.atom.javaapi;

import be.vlaanderen.awv.atom.FeedPosition;

public class FeedProcessingException extends RuntimeException {
    public final FeedPosition feedPosition;
    public FeedProcessingException(String message, FeedPosition feedPosition) {
        super(message);
        this.feedPosition = feedPosition;
    }
}
