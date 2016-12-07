package be.wegenenverkeer.atomium.store;

import java.sql.SQLException;

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
     * @return the highest {@code Entry} number after this indexer has run
     */
    public void index() throws SQLException;


}
