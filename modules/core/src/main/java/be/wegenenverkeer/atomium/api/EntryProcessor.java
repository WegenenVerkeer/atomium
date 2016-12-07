package be.wegenenverkeer.atomium.api;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
class EntryProcessor<T> implements Processor<Entry<T>, FeedPage<T>> {

    final private List<Entry<T>> entries;
    final private FeedPageBuilder<T> builder;
    final private long requested;
    private Subscription storeSubscription;

    private FeedPageSubscription<T> subscription;

    EntryProcessor(long requested, FeedPageBuilder<T> builder){
        this.requested = requested;
        entries = new ArrayList<>();
        this.builder = builder;
    }



    @Override
    public void onSubscribe(Subscription s) {
        this.storeSubscription = s;
    }

    @Override
    public void onNext(Entry<T> tEntry) {
        entries.add(tEntry);
    }

    @Override
    public void onError(Throwable t) {
        setError(t);
    }

    @Override
    public void onComplete() {
        this.builder.setEntries(entries);
        publishPage(this.builder.build());
    }

    @Override
    public void subscribe(Subscriber<? super FeedPage<T>> s) {
        if (subscription != null) {
            s.onError(new RuntimeException("Already subscribed"));
        }
        subscription = new FeedPageSubscription<>(s);
        storeSubscription.request(this.requested);
        s.onSubscribe(subscription);
    }

    void publishPage(FeedPage<T> page) {
        if (!subscription.isCancelled) {
            subscription.subscriber.onNext(page);
            subscription.subscriber.onComplete();
        }
    }

    void setError(Throwable t) {
        if (!subscription.isCancelled) {
            subscription.subscriber.onError(t);
        }
    }

    static class FeedPageSubscription<T> implements Subscription {
        boolean isCancelled = false;
        final Subscriber<? super FeedPage<T>> subscriber;

        FeedPageSubscription(Subscriber<? super FeedPage<T>> subscriber){
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            // ignored
        }

        @Override
        public void cancel() {
            isCancelled = true;
        }
    }
}



