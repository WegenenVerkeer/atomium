package be.wegenenverkeer.atomium.client;

public class FeedPositionStrategyFromNowOn extends DefaultFeedPositionStrategy {
    @Override
    protected <E> FeedPosition initialFeedPosition(CachedFeedPage<E> page) {
        return new FeedPosition(page.getSelfHref(), page.getMostRecentEntryId());
    }
}
