package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Generator;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Provides a single FeedPage
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public interface FeedPageProvider<T> {

    Publisher<FeedPage<T>> feedPage(FeedPageRef ref);

    /**
     * Return a reference to the most recent {@code FeedPage}}
     *
     * The head-of-feed {@Code FeedPage} can ber empty
     *
     * @return a {@code FeedPageRef} to the most recent {@code FeedPage}
     */
    FeedPageRef getHeadOfFeedRef();

    /**
     * Returns the page size, i.e. maximum number of elements in the feed
     * @return
     */
    long getPageSize();

    String getFeedUrl();

    String getFeedName();

    Generator getFeedGenerator();


    default Future<FeedPage<T>> getFeedPageAsync(FeedPageRef ref) {
        CompletableFuture<FeedPage<T>> fPage = new CompletableFuture<>();
        feedPage(ref).subscribe( new FeedPageToFutureSubscriber<>( fPage ) );
        return fPage;
    }

    default FeedPage<T> getFeedPage(FeedPageRef ref) {
        try {
            return getFeedPageAsync(ref).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            //if cause of execution exception is a runtime exception, rethrow it,
            if ( RuntimeException.class.isAssignableFrom( e.getCause().getClass())) {
                throw (RuntimeException) e.getCause();
            } else {
                //if not, first wrap in runtime exception, and rethrow
                throw new RuntimeException(e.getCause());
            }
        }
    }



}

