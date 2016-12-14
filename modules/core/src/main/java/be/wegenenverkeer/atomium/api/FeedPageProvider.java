package be.wegenenverkeer.atomium.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static be.wegenenverkeer.atomium.api.AsyncToSync.runAndWait;

/**
 * Provides a single FeedPage
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public interface FeedPageProvider<T> {

    /**
     * Return a reference to the most recent {@code FeedPage}}
     *
     * The head-of-feed {@Code FeedPage} can be empty
     *
     * @return a {@code Future<FeedPageRef>} to the most recent {@code FeedPage}
     */
    CompletableFuture<FeedPageRef> getHeadOfFeedRefAsync();

    CompletableFuture<FeedPage<T>> getFeedPageAsync(FeedPageRef ref);

    default FeedPage<T> getFeedPage(FeedPageRef ref) {
        return runAndWait( () -> getFeedPageAsync(ref) );
    }

    /**
     * Return a reference to the most recent {@code FeedPage}}
     *
     * The head-of-feed {@Code FeedPage} can be empty
     *
     * @return a {@code FeedPageRef} to the most recent {@code FeedPage}
     */
    default FeedPageRef getHeadOfFeedRef() {
        return runAndWait(() -> getHeadOfFeedRefAsync());
    }


}
