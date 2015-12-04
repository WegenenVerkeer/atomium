package be.wegenenverkeer.atomium.japi.client;

/**
 *  A functional interface for retry strategies
 *
 * Created by Karel Maesen, Geovise BVBA on 18/11/15.
 *
 */
public interface RetryStrategy {

    /**
     * Applies the retry strategy to an <code>Observable</code> of feed items.
     * <p>When an <code>Long</code> value is returned, it will be used as time in milliseconds to wait until the next attempt to retrieve the
     * feed page. If the exception is re-thrown the Observable will emit an Error with this exception.</p>
     * <p>If the exception passed is a checked exception, it will be wrapped in a <code>RuntimeException</code> before it is re-thrown.</p>
     * @param count number of consecutive exceptions received so far
     * @param exception exception currently received
     * @return Long the time in millisecs. to wait for next attempt to read the feed page
     */
    Long apply(Integer count, Throwable exception);

}
