package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.FeedEntryDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Karel Maesen, Geovise BVBA on 09/12/16.
 */
public interface JdbcFeedEntryDaoFactory<T> {

    public Codec<T, String> getEntryValueCodec();

    JdbcEntryStoreMetadata getJdbcEntryStoreMetadata();

    public JdbcDialect getDialect();

    default public FeedEntryDao<T> createDao(Connection conn) {
        return new JdbcFeedEntryDao<>(conn, getEntryValueCodec(), getJdbcEntryStoreMetadata(), getDialect());
    }
}

/**
 * Because Jdbc is inherently synchronous, we invert the priority: async methods derive from sync methods
 *
 * @param <T>
 */
class JdbcFeedEntryDao<T> implements FeedEntryDao<T> {

    final private Connection conn;
    final private Codec<T, String> codec;
    final private JdbcEntryStoreMetadata metadata;
    final private JdbcDialect dialect;

    JdbcFeedEntryDao(Connection c, Codec<T, String> codec, JdbcEntryStoreMetadata metadata, JdbcDialect dialect) {
        this.conn = c;
        this.codec = codec;
        this.metadata = metadata;
        this.dialect = dialect;
    }


    @Override
    public boolean push(List<Entry<T>> entries) {
        JdbcSaveEntryOp op = null;
        try {
            op = dialect.createSaveEntryOp(conn, codec, metadata);
            for (Entry<T> entry : entries) {
                op.set(entry);
                op.execute();
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally { //clean up
            if (op != null) {
                op.close();
            }
        }
    }

    @Override
    public boolean push(Entry<T> entry) {
        return push(Collections.singletonList(entry));
    }

    @Override
    public List<Entry<T>> getEntries(long startNum, long size) {
        JdbcGetEntriesOp<T> getEntriesOp = dialect.createGetEntriesOp(conn, codec, metadata);
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
