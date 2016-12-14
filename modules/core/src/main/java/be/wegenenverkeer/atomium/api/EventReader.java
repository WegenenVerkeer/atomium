package be.wegenenverkeer.atomium.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static be.wegenenverkeer.atomium.api.AsyncToSync.runAndWait;

/**
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public interface EventReader<T> {

    CompletableFuture<List<Event<T>>> getEventsAsync(long startNum, long size);

    CompletableFuture<Long> totalNumberOfEventsAsync() ;

    default List<Event<T>> getEvents(long startNum, long size) {
        return runAndWait(() -> getEventsAsync(startNum, size));
    }

    default Long totalNumberOfEvents() {
        return runAndWait(() -> totalNumberOfEventsAsync());
    }

}
