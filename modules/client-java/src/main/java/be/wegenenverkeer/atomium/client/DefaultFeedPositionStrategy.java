package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.core.Single;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public abstract class DefaultFeedPositionStrategy implements FeedPositionStrategy {

    private Duration pollingDelay = Duration.ofSeconds(1);

    public DefaultFeedPositionStrategy withPollingDelay(Duration pollingDelay) {
        this.pollingDelay = pollingDelay;
        return this;
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
