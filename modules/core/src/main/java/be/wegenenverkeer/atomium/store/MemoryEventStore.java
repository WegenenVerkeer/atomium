package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Event;
import be.wegenenverkeer.atomium.api.EventDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An in-memory store for entries, meant as implementation example, and
 * for use in tests
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class MemoryEventStore<T> implements EventDao<T> {

    final private ConcurrentSkipListMap<Long, Event<T>> store = new ConcurrentSkipListMap<>();
    final private AtomicLong counter = new AtomicLong(0);


    @Override
    public CompletableFuture<Boolean> pushAsync(List<Event<T>> events) {
        return CompletableFuture.completedFuture(this.push(events));
    }

    @Override
    public boolean push(List<Event<T>> entries) {
        entries.forEach(e -> store.put(counter.getAndIncrement(), e));
        return true;
    }

    @Override
    public CompletableFuture<List<Event<T>>> getEventsAsync(long startNum, long size) {
        Collection<Event<T>> coll = store.subMap(startNum, startNum + size).values();
        List<Event<T>> entries = new ArrayList<>(coll.size());
        entries.addAll(coll);
        return CompletableFuture.completedFuture(entries);
    }

    @Override
    public CompletableFuture<Long> totalNumberOfEventsAsync() {
        return CompletableFuture.completedFuture(counter.get());
    }


}


