package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.EntryDao;
import be.wegenenverkeer.atomium.format.AtomEntry;
import be.wegenenverkeer.atomium.format.Content;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class StoreFixture<T> {

    private AtomicInteger counter = new AtomicInteger(0);
    final public EntryDao<T> store;


    public StoreFixture() {
        this.store = new MemoryEntryStore<>();
    }

    public void loadEntries(int num, T value){
        for (int i = 0; i < num; i++) {
            store.push(mkEntry(value));
        }
    }

    public Entry<T> mkEntry(T val){
        return new AtomEntry<T>( Integer.toString(counter.getAndIncrement()) , new Content<T>(val, ""));
    }
}
