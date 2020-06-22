package be.wegenenverkeer.atomium.japi.client;

class FeedPositionStrategyFromStart extends DefaultFeedPositionStrategy {

    @Override
    protected <E> FeedPosition initialFeedPosition(CachedFeedPage<E> page) {
        return new FeedPosition(page.getOldestHref());
    }
}
