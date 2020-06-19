package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Single;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    public <E> Single<FeedPosition> getNextFeedPosition(CachedFeedPage<E> page, Duration pollingInterval) {
        Single<FeedPosition> nextFeedPosition = Single.just(FeedPositions.of(page.getPreviousHref().orElseGet(page::getSelfHref), page.getMostRecentEntryId()));

        if (page.getPreviousHref().isEmpty()) {
            nextFeedPosition = nextFeedPosition.delay(pollingInterval.toMillis(), TimeUnit.MILLISECONDS);
        }

        return nextFeedPosition;
    }

    @Override
    public String getPageUrl() {
        return this.pageUrl;
    }
}
