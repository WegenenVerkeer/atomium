package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Flowable;

import java.util.Optional;

public interface AtomiumFeed<E> {

    AtomiumFeed<E> withRetry(RetryStrategy retryStrategy);

    Flowable<FeedEntry<E>> fetchEntries(FeedPositionStrategy feedPositionStrategy, Optional<String> eTag);
}
