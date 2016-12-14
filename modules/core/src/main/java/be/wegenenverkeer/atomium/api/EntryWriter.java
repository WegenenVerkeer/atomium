package be.wegenenverkeer.atomium.api;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static be.wegenenverkeer.atomium.api.AsyncToSync.runAndWait;

/**
 * Writes {@code Entry}s to a store
 *
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public interface EntryWriter<T> {

    CompletableFuture<Boolean> pushAsync(List<Entry<T>> entries);

    default CompletableFuture<Boolean> pushAsync(Entry<T> entry) {
        return pushAsync(Collections.singletonList(entry));
    }

    default boolean push(List<Entry<T>> entries) {
        return runAndWait( () -> pushAsync(entries) );
    }

    default boolean push(Entry<T> entry) {
        return push(Collections.singletonList(entry));
    }

}
