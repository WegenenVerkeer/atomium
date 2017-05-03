package be.wegenenverkeer.atomium.api;

/**
 * Provides a single FeedPage
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public interface FeedPageProvider<T> {


    FeedPage<T> getFeedPage(FeedPageRef ref);

    /**
     * Return a reference to the most recent {@code FeedPage}}
     *
     * The head-of-feed {@code FeedPage} can be empty
     *
     * @return a {@code FeedPageRef} to the most recent {@code FeedPage}
     */
    FeedPageRef getHeadOfFeedRef();


}
