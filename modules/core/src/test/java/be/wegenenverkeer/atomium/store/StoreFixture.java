package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Event;
import be.wegenenverkeer.atomium.api.EventDao;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class StoreFixture<T> {

    private AtomicInteger counter = new AtomicInteger(0);
    final public EventDao<T> store;


    public StoreFixture() {
        this.store = new MemoryEventStore<>();
    }

    public void loadEntries(int num, T value){
        for (int i = 0; i < num; i++) {
            store.push(mkEntry(value));
        }
    }

    public Event<T> mkEntry(T val){
        return Event.make( Integer.toString(counter.getAndIncrement()) , val, OffsetDateTime.now());
    }
}
