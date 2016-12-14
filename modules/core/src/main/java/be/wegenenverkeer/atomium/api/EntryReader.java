package be.wegenenverkeer.atomium.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static be.wegenenverkeer.atomium.api.AsyncToSync.runAndWait;

/**
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public interface EntryReader<T> {

    CompletableFuture<List<Entry<T>>> getEntriesAsync(long startNum, long size);

    CompletableFuture<Long> totalNumberOfEntriesAsync() ;

    default List<Entry<T>> getEntries(long startNum, long size) {
        return runAndWait(() -> getEntriesAsync(startNum, size));
    }

    default Long totalNumberOfEntries() {
        return runAndWait(() -> totalNumberOfEntriesAsync());
    }

}
