package be.wegenenverkeer.atomium.client;

public class FeedProcessingException extends RuntimeException {

    final String entryId;

    public FeedProcessingException(String entryId, String message) {
        super(message);
        this.entryId = entryId;
    }

}
