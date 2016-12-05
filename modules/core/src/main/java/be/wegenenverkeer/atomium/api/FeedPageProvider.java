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

    Publisher<FeedPage<T>> feedPage(FeedPageReference ref);

    int getPageSize();

    String getFeedUrl();

    String getFeedName();

    Generator getFeedGenerator();

    default Future<FeedPage<T>> getFeedPageAsync(FeedPageReference ref) {
        CompletableFuture<FeedPage<T>> fPage = new CompletableFuture<>();
        feedPage(ref).subscribe( new FeedPageToFutureSubscriber<>( fPage ) );
        return fPage;
    }

    default FeedPage<T> getFeedPage(FeedPageReference ref) {
        try {
            return getFeedPageAsync(ref).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}

