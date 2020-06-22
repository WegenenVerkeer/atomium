package be.wegenenverkeer.atomium.japi.client;

import java.util.Optional;

public final class FeedPositionStrategies {
    private FeedPositionStrategies() {
    }

    public static FeedPositionStrategy from(String pageUrl, String entryId) {
        return new FeedPositionStrategyFrom(FeedPositions.of(pageUrl, entryId));
    }

    public static FeedPositionStrategy fromNowOn() {
        return new FeedPositionStrategyFromNowOn();
    }

    public static FeedPositionStrategy fromStart() {
        return new FeedPositionStrategyFromStart();
    }
}
