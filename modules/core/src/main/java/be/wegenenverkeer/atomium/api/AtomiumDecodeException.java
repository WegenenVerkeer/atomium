package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
public class AtomiumDecodeException extends RuntimeException {
    public AtomiumDecodeException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
