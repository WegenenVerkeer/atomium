package be.wegenenverkeer.atomium.store;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Indexes entries in a {@code JDBCFeedEntryStore}
 *
 * An {@code Indexer} will map entries in order on a (gapless) sequence 0...n with 0 the first registered Entry
 *
 * An {@code Indexer} should always run it's index operation in a separate transaction, and not in a transaction that also persists and/or
 * retrieves entries
 *
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
public interface Indexer {

    public boolean index(Connection conn) throws SQLException;
}
