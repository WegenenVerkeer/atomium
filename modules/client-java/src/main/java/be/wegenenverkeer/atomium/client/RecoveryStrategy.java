package be.wegenenverkeer.atomium.client;

/**
 *  A functional interface for recovery strategies
 *
 */
public interface RecoveryStrategy {

    /**
     * Applies a recovery an <code>Observable</code> of feed items. Will be called when the atomium feed's page-fetcher manages
     * to fetch a page again after a period of retry'ing.
     */
    void apply(int retriesBeforeSuccess);

}
