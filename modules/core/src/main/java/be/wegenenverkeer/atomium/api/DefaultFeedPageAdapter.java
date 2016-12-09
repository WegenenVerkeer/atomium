package be.wegenenverkeer.atomium.api;

import java.util.concurrent.CompletableFuture;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class DefaultFeedPageAdapter<T> implements FeedPageAdapter<T> {

    final private FeedEntryDao<T> entryDao;
    final private FeedPageMetadata metadata;

    public DefaultFeedPageAdapter(FeedEntryDao<T> dao, FeedPageMetadata meta){
        this.entryDao = dao;
        this.metadata = meta;
    }


    @Override
    public CompletableFuture<FeedPage<T>> getFeedPageAsync(FeedPageRef requestedPage) {


        if (requestedPage.isStrictlyMoreRecentThan(getHeadOfFeedRef())) {
            CompletableFuture<FeedPage<T>> futurePage = new CompletableFuture<>();
            futurePage.completeExceptionally(new IndexOutOfBoundsException("Requested page currently beyond head of feed"));
        }

        long pageSize = metadata.getPageSize();
        long requested = pageSize + 1;

        FeedPageBuilder<T> builder = new FeedPageBuilder<>(this.metadata, requestedPage.getPageNum());

        return entryDao.getEntriesAsync(requestedPage.getPageNum() * pageSize, requested).thenApply(entries -> builder.setEntries(entries).build());

    }

    @Override
    public CompletableFuture<FeedPageRef> getHeadOfFeedRefAsync() {
        return entryDao.totalNumberOfEntriesAsync().thenApply(n -> FeedPageRef.page( n / metadata.getPageSize()));
    }

}
