package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.FeedEntryDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 08/12/16.
 */
public class DefaultPgJdbcFeedEntryStore<T> implements JdbcFeedEntryDaoFactory<T> {


    private final Indexer indexer;
    private final Codec<T, String> codec;
    private final JdbcEntryStoreMetadata meta;


    public DefaultPgJdbcFeedEntryStore(JdbcEntryStoreMetadata meta, Codec<T, String> codec) {
        this.meta = meta;
        this.codec = codec;
        this.indexer = new PostgreSQLIndexer(meta);
    }

    public void index(Connection conn) throws SQLException {
        this.indexer.index(conn);
    }

    public List<Entry<T>> indexAndRetrieve(Connection conn, long startNum, long size) throws SQLException {
        if (!conn.getAutoCommit()) throw new IllegalArgumentException("This method requires auto-commit mode on the connection");
        index(conn);
        FeedEntryDao<T> dao = createDao(conn);
        return dao.getEntries(startNum, size);
    }

    @Override
    public Codec<T, String> getEntryValueCodec() {
        return this.codec;
    }

    @Override
    public JdbcEntryStoreMetadata getJdbcEntryStoreMetadata() {
        return this.meta;
    }

    @Override
    public JdbcDialect getDialect() {
        return PgJdbcDialect.INSTANCE;
    }

}

