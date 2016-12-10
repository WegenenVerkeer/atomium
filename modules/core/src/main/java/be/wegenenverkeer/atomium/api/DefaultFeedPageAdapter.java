package be.wegenenverkeer.atomium.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class DefaultFeedPageAdapter<T> implements FeedPageAdapter<T> {

    final private FeedEntryDao<T> entryDao;
    final private FeedPageMetadata metadata;


    public DefaultFeedPageAdapter(FeedEntryDao<T> dao, FeedPageMetadata meta) {
        this.entryDao = dao;
        this.metadata = meta;
    }

    @Override
    public CompletableFuture<FeedPage<T>> getFeedPageAsync(FeedPageRef requestedPage) {


        return getHeadOfFeedRefAsync().thenCompose(headOfFeed -> {
            if (requestedPage.isStrictlyMoreRecentThan(headOfFeed)) {
                return indexOutOfBound();
            } else  return mkFeedPage(requestedPage);
        });

    }

    private CompletionStage<FeedPage<T>> indexOutOfBound() {
        CompletableFuture<FeedPage<T>> result = new CompletableFuture<>();
        result.completeExceptionally(new IndexOutOfBoundsException("Requested page currently beyond head of feed"));
        return result;
    }

    private CompletableFuture<FeedPage<T>> mkFeedPage(FeedPageRef requestedPage) {

        long pageSize = metadata.getPageSize();
        long requested = pageSize + 1;

        FeedPageBuilder<T> builder = new FeedPageBuilder<>(this.metadata, requestedPage.getPageNum());

        return entryDao.getEntriesAsync(requestedPage.getPageNum() * pageSize, requested)
                .thenApply(entries -> builder.setEntries(entries).build());

    }

    @Override
    public CompletableFuture<FeedPageRef> getHeadOfFeedRefAsync() {
        return entryDao.totalNumberOfEntriesAsync().thenApply(n -> FeedPageRef.page(n / metadata.getPageSize()));
    }

}
