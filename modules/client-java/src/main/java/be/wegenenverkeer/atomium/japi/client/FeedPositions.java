package be.wegenenverkeer.atomium.japi.client;

import java.util.Optional;

public final class FeedPositions {
    private FeedPositions() {
    }

    public static FeedPosition of(String pageUrl) {
        return new FeedPosition(pageUrl);
    }

    public static FeedPosition of(String pageUrl, String entryId) {
        return new FeedPosition(pageUrl, entryId);
    }

    public static FeedPosition of(String pageUrl, Optional<String> entryIdOpt) {
        return entryIdOpt.map(entryId -> new FeedPosition(pageUrl, entryId)).orElseGet(() -> new FeedPosition(pageUrl));
    }

    public static <E> FeedPosition ofMostRecentEntry(CachedFeedPage<E> page) {
        return new FeedPosition(page.getSelfHref(), page.getMostRecentEntryId());
    }

    public static <E> FeedPosition ofOldest(CachedFeedPage<E> page) {
        return new FeedPosition(page.getLastHref());
    }
}
