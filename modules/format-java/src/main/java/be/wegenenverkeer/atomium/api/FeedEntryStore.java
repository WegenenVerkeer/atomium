package be.wegenenverkeer.atomium.api;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public interface FeedEntryStore<T> {

    void add(List<Entry<T>> entries);

    void add(Entry<T>... entries);

    Publisher<Entry<T>> getEntries(int startNum, int size);

}
