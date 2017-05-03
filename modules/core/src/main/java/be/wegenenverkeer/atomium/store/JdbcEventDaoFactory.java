package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A factory for {@code EntryDao}s
 *
 * Created by Karel Maesen, Geovise BVBA on 09/12/16.
 */
public interface JdbcEventDaoFactory<T> {

    Codec<T, String> getEntryValueCodec();

    JdbcEventStoreMetadata getJdbcEntryStoreMetadata();

    JdbcDialect getDialect();

    default EventDao<T> createDao(Connection conn) {
        return new JdbcEventDao<>(conn, getEntryValueCodec(), getJdbcEntryStoreMetadata(), getDialect());
    }

    default EventReader<T> createReader(Connection conn) {
        return createDao(conn);
    }

    default EventWriter<T> createWriter(Connection conn) {
        return createDao(conn);
    }

}

/**
 * Because Jdbc is inherently synchronous, we invert the priority: async methods derive from sync methods
 *
 * @param <T>
 */
class JdbcEventDao<T> implements EventDao<T> {

    final private Connection conn;
    final private Codec<T, String> codec;
    final private JdbcEventStoreMetadata metadata;
    final private JdbcDialect dialect;

    JdbcEventDao(Connection c, Codec<T, String> codec, JdbcEventStoreMetadata metadata, JdbcDialect dialect) {
        this.conn = c;
        this.codec = codec;
        this.metadata = metadata;
        this.dialect = dialect;
    }


    @Override
    public boolean push(List<Event<T>> events) {
        try (SaveEventOp<T> op = dialect.mkSaveEventOp(conn, codec, metadata) ){
            for (Event<T> event : events) {
                op.set(event);
                op.execute();
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean push(Event<T> event) {
        return push(Collections.singletonList(event));
    }

    @Override
    public List<Event<T>> getEvents(long startNum, long size) {
        GetEventsOp<T> getEventsOp = dialect.mkGetEventsOp(conn, codec, metadata);
        getEventsOp.setRange(startNum, size);
        return runOp(getEventsOp);
    }

    @Override
    public Long totalNumberOfEvents() {
        return runOp(dialect.mkTotalSizeOp(conn, metadata));
    }

    private <R> R runOp(JdbcOp<R> op) {
        try {
            return op.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (op != null) op.close();
        }
    }


}
