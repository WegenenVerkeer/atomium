package be.wegenenverkeer.atomium.api;

import java.util.List;

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

        private FeedPage<T> mkFeedPage(FeedPageRef requestedPage) {

            long pageSize = metadata.getPageSize();
            long requested = pageSize + 1;

            FeedPageBuilder<T> builder = new FeedPageBuilder<>(this.metadata, requestedPage.getPageNum());

            List<Event<T>> events = eventDao.getEvents(requestedPage.getPageNum() * pageSize, requested);
            return builder.setEvents(events).build();

        }

        @Override
        public FeedPage<T> getFeedPage(FeedPageRef ref) {
            FeedPageRef headOfFeed = getHeadOfFeedRef();
            if (ref.isStrictlyMoreRecentThan(headOfFeed)) {
                throw new IndexOutOfBoundsException("Requested page currently beyond head of feed");
            }
            return mkFeedPage(ref);
        }

        /**
         * Return a reference to the most recent {@code FeedPage}}
         * <p>
         * The head-of-feed {@code FeedPage} can be empty
         *
         * @return a {@code FeedPageRef} to the most recent {@code FeedPage}
         */
        @Override
        public FeedPageRef getHeadOfFeedRef() {
            return FeedPageRef.page(eventDao.totalNumberOfEvents() / metadata.getPageSize());
        }
    }


}

