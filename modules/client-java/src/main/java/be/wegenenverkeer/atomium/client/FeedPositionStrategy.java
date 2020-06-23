package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.core.Single;

public interface FeedPositionStrategy {
    <E> Single<FeedPosition> getNextFeedPosition(CachedFeedPage<E> feedPage);
}
