package be.wegenenverkeer.atomium.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Utility class for turning Aync operations into Synchronous ops.
 *
 * Created by Karel Maesen, Geovise BVBA on 08/12/16.
 */
public class AsyncToSync {

    public static <T> T runAndWait( Supplier<CompletableFuture<T>> async ) {
        try {
            return async.get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw toRuntimeException(e);
        }

    }

    public static RuntimeException toRuntimeException(ExecutionException e) {
        //if cause of execution exception is a runtime exception, rethrow it,
        if ( RuntimeException.class.isAssignableFrom( e.getCause().getClass())) {
            return (RuntimeException) e.getCause();
        } else {
            //if not, first wrap in runtime exception, and rethrow
            return new RuntimeException(e.getCause());
        }
    }

}
