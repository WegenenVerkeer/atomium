package be.wegenenverkeer.atomium.api;

import java.util.concurrent.ExecutionException;

/**
 * Created by Karel Maesen, Geovise BVBA on 08/12/16.
 */
class ExecutionExceptionUnpacker {

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
