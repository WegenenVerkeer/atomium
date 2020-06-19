package be.wegenenverkeer.atomium.japi.client;

import java.util.Optional;

class FeedPositionImpl implements FeedPosition {
    private final Optional<String> entryId;
    private final String pageUrl;

    FeedPositionImpl(String pageUrl) {
        this(pageUrl, null);
    }

    FeedPositionImpl(String pageUrl, String entryId) {
        this.entryId = Optional.ofNullable(entryId);
        this.pageUrl = pageUrl;
    }

    @Override
    public Optional<String> getEntryId() {
        return this.entryId;
    }

    @Override
    public String getPageUrl() {
        return this.pageUrl;
    }
}
