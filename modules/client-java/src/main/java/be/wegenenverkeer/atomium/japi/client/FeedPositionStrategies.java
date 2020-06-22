package be.wegenenverkeer.atomium.japi.client;

import java.util.Optional;

public final class FeedPositionStrategies {
    private FeedPositionStrategies() {
    }

    public static DefaultFeedPositionStrategy from(String pageUrl, String entryId) {
        return new FeedPositionStrategyFrom(FeedPositions.of(pageUrl, entryId));
    }

    public static DefaultFeedPositionStrategy fromNowOn() {
        return new FeedPositionStrategyFromNowOn();
    }

    public static DefaultFeedPositionStrategy fromStart() {
        return new FeedPositionStrategyFromStart();
    }
}
