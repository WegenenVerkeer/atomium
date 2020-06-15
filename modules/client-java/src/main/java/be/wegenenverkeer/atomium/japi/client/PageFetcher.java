package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Single;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;

public interface PageFetcher<E> {
    Single<CachedFeedPage<E>> fetch(String url, Optional<String> etag);
    Duration getPollingInterval();
    void close();
}
