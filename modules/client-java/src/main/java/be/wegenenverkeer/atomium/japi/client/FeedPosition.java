package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Single;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

class FeedPosition {
    String pageUrl;
    String entryId;

    public FeedPosition(String pageUrl, String entryId) {
        this.pageUrl = pageUrl;
        this.entryId = entryId;
    }

    public FeedPosition(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getEntryId() {
        return entryId;
    }
}

interface FeedPositionStrategy {
    <E> Single<FeedPosition> getNextFeedPosition(CachedFeedPage<E> feedPage);
}

abstract class DefaultFeedPositionStrategy implements FeedPositionStrategy {

    private Duration pollingDelay = Duration.ofSeconds(10);

    public void setPollingDelay(Duration pollingDelay) {
        this.pollingDelay = pollingDelay;
    }

    private boolean starting = true;

    @Override
    public <E> Single<FeedPosition> getNextFeedPosition(CachedFeedPage<E> page) {
        if (starting) {
            starting = false;
            return Single.just(initialFeedPosition(page));
        }

        return page.getPreviousHref()
                .map(previousHref -> Single.just(new FeedPosition(previousHref)))
                .orElseGet(() -> Single.just(new FeedPosition(page.getSelfHref(), page.getMostRecentEntryId()))
                        .delay(pollingDelay.toMillis(), TimeUnit.MILLISECONDS));
    }

    protected abstract <E> FeedPosition initialFeedPosition(CachedFeedPage<E> page);
}

class FeedPositionStrategyFromStart extends DefaultFeedPositionStrategy {

    @Override
    protected <E> FeedPosition initialFeedPosition(CachedFeedPage<E> page) {
        return new FeedPosition(page.getOldestHref());
    }
}

class FeedPositionStrategyFromNowOn extends DefaultFeedPositionStrategy {

    @Override
    protected <E> FeedPosition initialFeedPosition(CachedFeedPage<E> page) {
        return new FeedPosition(page.getSelfHref(), page.getMostRecentEntryId());
    }
}

class FeedPositionStrategyFrom extends DefaultFeedPositionStrategy {

    private final FeedPosition feedPosition;

    public FeedPositionStrategyFrom(FeedPosition feedPosition) {
        this.feedPosition = feedPosition;
    }

    @Override
    protected <E> FeedPosition initialFeedPosition(CachedFeedPage<E> page) {
        return feedPosition;
    }
}
