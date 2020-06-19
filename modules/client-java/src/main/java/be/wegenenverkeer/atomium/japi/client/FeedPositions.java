package be.wegenenverkeer.atomium.japi.client;

import java.util.Optional;

public final class FeedPositions {
    private FeedPositions() {
    }

    public static FeedPosition of(String pageUrl) {
        return new FeedPositionImpl(pageUrl);
    }

    public static FeedPosition of(String pageUrl, String entryId) {
        return new FeedPositionImpl(pageUrl, entryId);
    }

    public static FeedPosition of(String pageUrl, Optional<String> entryIdOpt) {
        return entryIdOpt.map(entryId -> new FeedPositionImpl(pageUrl, entryId)).orElseGet(() -> new FeedPositionImpl(pageUrl));
    }

    public static <E> FeedPosition ofMostRecentEntry(CachedFeedPage<E> page) {
        return new FeedPositionImpl(page.getSelfHref(), page.getMostRecentEntryId());
    }

    public static <E> FeedPosition ofBeginning(CachedFeedPage<E> page) {
        return new FeedPositionImpl(page.getLastHref());
    }
}
