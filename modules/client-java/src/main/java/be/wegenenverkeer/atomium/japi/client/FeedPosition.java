package be.wegenenverkeer.atomium.japi.client;

import java.util.Optional;

public interface FeedPosition {
    String getPageUrl();
    Optional<String> getEntryId();
}
