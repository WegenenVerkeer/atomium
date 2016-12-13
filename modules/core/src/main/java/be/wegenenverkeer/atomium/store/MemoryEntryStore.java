package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.EntryDao;

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
public class MemoryEntryStore<T> implements EntryDao<T> {

    final private ConcurrentSkipListMap<Long, Entry<T>> store = new ConcurrentSkipListMap<>();
    final private AtomicLong counter = new AtomicLong(0);


    @Override
    public CompletableFuture<Boolean> pushAsync(List<Entry<T>> entries) {
        return CompletableFuture.completedFuture(this.push(entries));
    }

    @Override
    public boolean push(List<Entry<T>> entries) {
        entries.forEach(e -> store.put(counter.getAndIncrement(), e));
        return true;
    }

    @Override
    public CompletableFuture<List<Entry<T>>> getEntriesAsync(long startNum, long size) {
        Collection<Entry<T>> coll = store.subMap(startNum, startNum + size).values();
        List<Entry<T>> entries = new ArrayList<>(coll.size());
        entries.addAll(coll);
        return CompletableFuture.completedFuture(entries);
    }

    @Override
    public CompletableFuture<Long> totalNumberOfEntriesAsync() {
        return CompletableFuture.completedFuture(counter.get());
    }


}


