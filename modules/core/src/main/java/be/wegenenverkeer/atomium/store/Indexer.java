package be.wegenenverkeer.atomium.store;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Indexes entries in a {@code JDBCFeedEntryStore}
 *
 * An {@code Indexer} will map entries in order on a (gapless) sequence 0...n with 0 the first registered Entry
 *
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
public interface Indexer {

    /**
     * Runs the indexer
     *
     * @return a Future that completes successfully when the indexing has been done
     */
    public CompletableFuture<Boolean> index() throws SQLException;


}
