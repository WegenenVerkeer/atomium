package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.FeedEntryStore;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An in-memory store for entries, meant as implementation example, and
 * for use in tests
 *
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class MemoryFeedEntryStore<T> implements FeedEntryStore<T> {

    final ConcurrentSkipListMap<Long, Entry<T>> store = new ConcurrentSkipListMap<>();
    final AtomicLong counter = new AtomicLong(0);

    @Override
    public void push(List<Entry<T>> entries) {
        entries.forEach(e -> store.put(counter.getAndIncrement(), e));
    }

    @Override
    public Publisher<Entry<T>> getEntries(long startNum, long size) {
        Collection<Entry<T>> coll = store.subMap(startNum, startNum + size).values();
        List<Entry<T>> entries = new ArrayList(coll.size());
        entries.addAll(coll);
        return toPublisher(entries);
    }

    @Override
    public long totalNumberOfEntries() {
        return counter.get();
    }

    private Publisher<Entry<T>> toPublisher(List<Entry<T>> entries) {
        return new SimpleSynchronousEntryPublisher<T>(entries);
    }

    //These are simplistic implementations of Reactive Streams, they are only here to not introduce a dependency on
    // a specific ReactiveStreams implementation
    static class SimpleSynchronousEntryPublisher<T>  implements Publisher<Entry<T>> {
        final List<Entry<T>> entries;
        SimpleSubscription subscription;

        SimpleSynchronousEntryPublisher(List<Entry<T>> entries){
            this.entries = entries;
        }

        @Override
        public void subscribe(Subscriber<? super Entry<T>> s) {
            this.subscription = new SimpleSubscription(this, s);
            s.onSubscribe(this.subscription);
        }

        List<Entry<T>> getEntries(long firstInclusive, long size) {
            int last = Math.min((int) (firstInclusive + size), this.entries.size());
            return this.entries.subList((int)firstInclusive, last);
        }
    }

    static class SimpleSubscription<T> implements Subscription {

        boolean isCanceled = false;
        long next = 0;
        final SimpleSynchronousEntryPublisher<T> publisher;
        final Subscriber<Entry<T>> subscriber;

        SimpleSubscription(SimpleSynchronousEntryPublisher<T> publisher, Subscriber<Entry<T>> subscriber) {
            this.subscriber = subscriber;
            this.publisher = publisher;
        }

        @Override
        public void request(long n) {
            if (!isCanceled) {
                for (Entry<T> entry : this.publisher.getEntries(next, n)) {
                    this.subscriber.onNext(entry);
                }
                this.next = next + n;
                if (next >= this.publisher.entries.size()) {
                    this.subscriber.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            isCanceled = true;
        }
    }
}


