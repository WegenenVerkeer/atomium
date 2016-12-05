package be.wegenenverkeer.atomium.api;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
class FeedPageToFutureSubscriber<T> implements Subscriber<FeedPage<T>> {

    final private CompletableFuture<FeedPage<T>> fPage;
    FeedPageToFutureSubscriber(CompletableFuture<FeedPage<T>> fPage) {
        this.fPage = fPage;
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(1);
    }

    @Override
    public void onNext(FeedPage<T> tFeedPage) {
        fPage.complete(tFeedPage);
    }

    @Override
    public void onError(Throwable t) {
        fPage.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        if (!fPage.isDone()){
            fPage.completeExceptionally(new IllegalStateException("FeedPage publisher completed without returning a page."));
        }
    }
}