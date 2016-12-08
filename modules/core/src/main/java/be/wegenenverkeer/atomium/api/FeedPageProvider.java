package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Generator;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static be.wegenenverkeer.atomium.api.ExecutionExceptionUnpacker.toRuntimeException;

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

    /**
     * Returns the page size, i.e. maximum number of elements in the feed
     * @return
     */
    long getPageSize();

    String getFeedUrl();

    String getFeedName();

    Generator getFeedGenerator();


    CompletableFuture<FeedPage<T>> getFeedPageAsync(FeedPageRef ref);

    default FeedPage<T> getFeedPage(FeedPageRef ref) {
        try {
            return getFeedPageAsync(ref).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw toRuntimeException(e);
        }
    }

    /**
     * Return a reference to the most recent {@code FeedPage}}
     *
     * The head-of-feed {@Code FeedPage} can be empty
     *
     * @return a {@code FeedPageRef} to the most recent {@code FeedPage}
     */
    default FeedPageRef getHeadOfFeedRef() {
        try {
            return getHeadOfFeedRefAsync().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw toRuntimeException(e);
        }
    }



}

