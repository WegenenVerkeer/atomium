package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Single;

import java.time.Duration;
import java.util.Optional;

public interface FeedPosition {
    String getPageUrl();
    Optional<String> getEntryId();
    <E> Single<FeedPosition> getNextFeedPosition(CachedFeedPage<E> feedPage, Duration pollingInterval);
}
