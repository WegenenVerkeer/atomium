package be.wegenenverkeer.atomium.api;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static be.wegenenverkeer.atomium.api.AsyncToSync.runAndWait;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 *
 * @param <T> entry value type*
 */
public interface FeedEntryDao<T> {

    CompletableFuture<Boolean> pushAsync(List<Entry<T>> entries);

    CompletableFuture<List<Entry<T>>> getEntriesAsync(long startNum, long size);

    CompletableFuture<Long> totalNumberOfEntriesAsync() ;

    default CompletableFuture<Boolean> pushAsync(Entry<T> entry) {
        return pushAsync(Collections.singletonList(entry));
    }

    default boolean push(List<Entry<T>> entries) {
        return runAndWait( () -> pushAsync(entries) );
    }

    default boolean push(Entry<T> entry) {
        return push(Collections.singletonList(entry));
    }

    default List<Entry<T>> getEntries(long startNum, long size) {
        return runAndWait(() -> getEntriesAsync(startNum, size));
    }

    default Long totalNumberOfEntries() {
        return runAndWait(() -> totalNumberOfEntriesAsync());
    }

}
