package be.wegenenverkeer.atomium.client;

public class FeedPositionStrategyFromStart extends DefaultFeedPositionStrategy {
    @Override
    protected <E> FeedPosition initialFeedPosition(CachedFeedPage<E> page) {
        return new FeedPosition(page.getLastHref());
    }
}
