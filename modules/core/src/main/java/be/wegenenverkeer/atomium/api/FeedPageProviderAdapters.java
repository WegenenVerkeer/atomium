package be.wegenenverkeer.atomium.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public class FeedPageProviderAdapters {

    public static <T> FeedPageProvider<T> adapt(EventDao<T> dao, FeedMetadata meta) {
        return new DaoBackedFeeedPageProvider<>(dao, meta);
    }


    static class DaoBackedFeeedPageProvider<T> implements FeedPageProvider<T> {

        final private EventDao<T> eventDao;
        final private FeedMetadata metadata;


        DaoBackedFeeedPageProvider(EventDao<T> dao, FeedMetadata meta) {
            this.eventDao = dao;
            this.metadata = meta;
        }

        @Override
        public CompletableFuture<FeedPage<T>> getFeedPageAsync(FeedPageRef requestedPage) {


            return getHeadOfFeedRefAsync().thenCompose(headOfFeed -> {
                if (requestedPage.isStrictlyMoreRecentThan(headOfFeed)) {
                    return indexOutOfBound();
                } else return mkFeedPage(requestedPage);
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

            return eventDao.getEventsAsync(requestedPage.getPageNum() * pageSize, requested)
                    .thenApply(entries -> builder.setEvents(entries).build());

        }

        @Override
        public CompletableFuture<FeedPageRef> getHeadOfFeedRefAsync() {
            return eventDao.totalNumberOfEventsAsync().thenApply(n -> FeedPageRef.page(n / metadata.getPageSize()));
        }

    }


}

