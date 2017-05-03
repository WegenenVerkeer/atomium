package be.wegenenverkeer.atomium.store;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Indexes events in a {@code JDBCFeedEntryStore}
 *
 * An {@code Indexer} will map events in order on a (gapless) sequence 0...n with 0 the first registered Event
 *
 * An {@code Indexer} should always run it's index operation in a separate transaction, and not in a transaction that also persists and/or
 * retrieves Events
 *
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
public class Indexer {

    final private JdbcDialect dialect;
    final private JdbcEventStoreMetadata meta;
    Indexer(JdbcDialect dialect, JdbcEventStoreMetadata metadata){
        this.dialect = dialect;
        this.meta = metadata;
    }

    public boolean index(Connection conn) throws SQLException {
        try (IndexOp op = dialect.mkIndexOp(conn, this.meta)) {
            return op.execute();
        }
    }
}
