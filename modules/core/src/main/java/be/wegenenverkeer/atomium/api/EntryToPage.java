package be.wegenenverkeer.atomium.api;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
class EntryToPage<T> implements Subscriber<Entry<T>> {

    final private List<Entry<T>> entries;
    final private FeedPageBuilder<T> builder;
    final private CompletableFuture<FeedPage<T>> futurePage;
    final private long requested;


    private Subscription storeSubscription;

    EntryToPage(long requested, FeedPageBuilder<T> builder, CompletableFuture<FeedPage<T>> futurePage) {
        this.requested = requested;
        entries = new ArrayList<>();
        this.builder = builder;
        this.futurePage = futurePage;
    }


    @Override
    public void onSubscribe(Subscription s) {
        this.storeSubscription = s;
        s.request(this.requested);
    }

    @Override
    public void onNext(Entry<T> tEntry) {
        entries.add(tEntry);
    }

    @Override
    public void onError(Throwable t) {
        futurePage.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        this.builder.setEntries(entries);
        try {
            FeedPage<T> result = this.builder.build();
            futurePage.complete(result);
        } catch (Throwable t) {
            futurePage.completeExceptionally(t);
        }
    }

}



