package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.core.Single;

import java.util.Optional;

public interface PageFetcher<E> {
    Single<CachedFeedPage<E>> fetch(String url, Optional<String> etag);
    void close();
    Class<E> getEntryTypeMarker();
    RetryStrategy getRetryStrategy();
    RecoveryStrategy getRecoveryStrategy();
}
