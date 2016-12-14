package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Event;
import be.wegenenverkeer.atomium.api.EventDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 08/12/16.
 */
public class PostgresEventStore<T> implements JdbcEventDaoFactory<T> {


    private final Indexer indexer;
    private final Codec<T, String> codec;
    private final JdbcEntryStoreMetadata meta;
    private final JdbcDialect dialect = PostgresDialect.INSTANCE;


    public PostgresEventStore(JdbcEntryStoreMetadata meta, Codec<T, String> codec) {
        this.meta = meta;
        this.codec = codec;
        this.indexer = new Indexer(dialect, meta);
    }

    public void index(Connection conn) throws SQLException {
        this.indexer.index(conn);
    }

    public List<Event<T>> indexAndRetrieve(Connection conn, long startNum, long size) throws SQLException {
        if (!conn.getAutoCommit()) throw new IllegalArgumentException("This method requires auto-commit mode on the connection");
        index(conn);
        EventDao<T> dao = createDao(conn);
        return dao.getEvents(startNum, size);
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
        return dialect;
    }

}

