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
public interface JdbcEntryDaoFactory<T> {

    Codec<T, String> getEntryValueCodec();

    JdbcEntryStoreMetadata getJdbcEntryStoreMetadata();

    JdbcDialect getDialect();

    default EntryDao<T> createDao(Connection conn) {
        return new JdbcEntryDao<>(conn, getEntryValueCodec(), getJdbcEntryStoreMetadata(), getDialect());
    }

    default EntryReader<T> createReader(Connection conn) {
        return createDao(conn);
    }

    default EntryWriter<T> createWriter(Connection conn) {
        return createDao(conn);
    }

}

/**
 * Because Jdbc is inherently synchronous, we invert the priority: async methods derive from sync methods
 *
 * @param <T>
 */
class JdbcEntryDao<T> implements EntryDao<T> {

    final private Connection conn;
    final private Codec<T, String> codec;
    final private JdbcEntryStoreMetadata metadata;
    final private JdbcDialect dialect;

    JdbcEntryDao(Connection c, Codec<T, String> codec, JdbcEntryStoreMetadata metadata, JdbcDialect dialect) {
        this.conn = c;
        this.codec = codec;
        this.metadata = metadata;
        this.dialect = dialect;
    }


    @Override
    public boolean push(List<Entry<T>> entries) {
        try (SaveEntryOp<T> op = dialect.createSaveEntryOp(conn, codec, metadata) ){
            for (Entry<T> entry : entries) {
                op.set(entry);
                op.execute();
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean push(Entry<T> entry) {
        return push(Collections.singletonList(entry));
    }

    @Override
    public List<Entry<T>> getEntries(long startNum, long size) {
        GetEntriesOp<T> getEntriesOp = dialect.createGetEntriesOp(conn, codec, metadata);
        getEntriesOp.setRange(startNum, size);
        return runOp( getEntriesOp );
    }

    @Override
    public Long totalNumberOfEntries() {
        return runOp(dialect.createTotalSizeOp(conn, metadata));
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

    // Derived Asynchronous implementations
    // we use CompletableFuture#supplyAsync() in order to have thrown (SQL)Exceptions handled correctly, i.e. by completing  a Future exceptionally

    @Override
    public CompletableFuture<Boolean> pushAsync(final Entry<T> entry) {
        return CompletableFuture.supplyAsync(() -> push(entry));
    }


    @Override
    public CompletableFuture<Boolean> pushAsync(final List<Entry<T>> entries) {
        return CompletableFuture.supplyAsync((() -> push(entries)));
    }

    @Override
    public CompletableFuture<List<Entry<T>>> getEntriesAsync(final long startNum, final long size) {
        return CompletableFuture.supplyAsync((() -> getEntries(startNum, size)));
    }

    @Override
    public CompletableFuture<Long> totalNumberOfEntriesAsync() {
        return CompletableFuture.supplyAsync((this::totalNumberOfEntries));
    }


}
