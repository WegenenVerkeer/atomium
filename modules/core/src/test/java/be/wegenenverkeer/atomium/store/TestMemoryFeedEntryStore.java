package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.format.AtomEntry;
import be.wegenenverkeer.atomium.format.Content;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class TestMemoryFeedEntryStore {


    private MemoryFeedEntryStore store;

    private AtomicInteger counter = new AtomicInteger(0);

    @Before
    public void setUp(){
        this.store = new MemoryFeedEntryStore();
    }

    @Test
    public void testMemoryFeedStore(){
        loadEntries(1000);
        CollectSubscriber<Object> s = new CollectSubscriber<>();
        store.getEntries(0, 11).subscribe( s );
        s.request(5);
        assertEquals(5, s.entries.size());
        s.request(6);
        assertEquals(11, s.entries.size());
        assertTrue(s.isCompleted);
        Stream<Integer> integerStream = s.entries.stream().map(e -> Integer.parseInt(e.getId()));
        List<Integer> received = integerStream.collect(Collectors.toList());
        Stream<Integer> intStream = (IntStream.range(0, 11)).boxed();
        List<Integer> expected = intStream.collect(Collectors.toList());
        assertEquals(expected, received);

    }


    @Test
    public void testMemoryFeedStoreCanRequestMore(){
        loadEntries(5);
        CollectSubscriber<Object> s = new CollectSubscriber<>();
        store.getEntries(0, 11).subscribe( s );
        s.request(100);
        assertTrue(s.isCompleted);
    }


    void loadEntries(int num){
        for (int i = 0; i < num; i++) {
            store.push(mkEntry());
        }
    }

    Entry<String> mkEntry(){
        return new AtomEntry<String>( Integer.toString(counter.getAndIncrement()) , new Content<String>("Some value", "test"));
    }
}


class CollectSubscriber<T> implements Subscriber<Entry<T>> {

    Subscription subscription;
    List<Entry<T>> entries = new ArrayList<>();
    boolean isCompleted = false;

    @Override
    public void onSubscribe(Subscription s) {
        subscription = s;
    }

    /**
     * Data notification sent by the {@link Publisher} in response to requests to {@link Subscription#request(long)}.
     *
     * @param t the element signaled
     */
    @Override
    public void onNext(Entry<T> t) {
        entries.add(t);
    }

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     *
     * @param t the throwable signaled
     */
    @Override
    public void onError(Throwable t) {
        throw new RuntimeException(t);
    }

    /**
     * Successful terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     */
    @Override
    public void onComplete() {
        isCompleted = true;
    }

    public void request(int n){
        this.subscription.request(n);
    }

}