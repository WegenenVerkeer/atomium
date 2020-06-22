package be.wegenenverkeer.atomium.japi.client;

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
