package be.wegenenverkeer.atomium.api;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static be.wegenenverkeer.atomium.api.AsyncToSync.runAndWait;

/**
 * Writes {@code Entry}s to a store
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public interface EventWriter<T> {

    CompletableFuture<Boolean> pushAsync(List<Event<T>> events);

    default CompletableFuture<Boolean> pushAsync(Event<T> event) {
        return pushAsync(Collections.singletonList(event));
    }

    default boolean push(List<Event<T>> events) {
        return runAndWait(() -> pushAsync(events));
    }

    default boolean push(Event<T> event) {
        return push(Collections.singletonList(event));
    }

}
