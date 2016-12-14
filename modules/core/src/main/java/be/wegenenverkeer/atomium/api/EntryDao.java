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
public interface EntryDao<T> extends EntryWriter<T>, EntryReader<T> {

}
