package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.format.AtomEntry;
import be.wegenenverkeer.atomium.format.Content;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public class PostgresDialect implements JdbcDialect {


    public static final PostgresDialect INSTANCE = new PostgresDialect();

    final public static String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS %s ( "
            + " %s SERIAL primary key, "
            + " %s INT, "
            +  "%s VARCHAR(60), "
            + " %s TIMESTAMP, "
            + " %s TEXT )";

    final public static String INSERT_STATEMENT = "INSERT INTO %s ( %s, %s, %s) VALUES (?, ?, ?)";

    final public static String MAX_SEQNO_STATEMENT = "SELECT MAX( %s ) FROM %s";

    final public static String SELECT_STATEMENT = "SELECT %s, %s, %s FROM %s WHERE %s >= ? ORDER BY %s LIMIT ?";

    @Override
    public <T> GetEntriesOp createGetEntriesOp(final Connection conn, final Codec<T, String> codec, final JdbcEntryStoreMetadata meta) {

        return new GetEntriesOp<T>() {
            private long startNum;
            private long size;

            @Override
            public void setRange(long startNum, long size) {
                this.startNum = startNum;
                this.size = size;
            }

            final String sql = String.format( SELECT_STATEMENT,
                    meta.getIdColumnName(),
                    meta.getEntryValueColumnName(),
                    meta.getUpdatedColumnName(),
                    meta.getTableName(),
                    meta.getSequenceNoColumnName(),
                    meta.getSequenceNoColumnName());


            @Override
            public List<Entry<T>> execute() throws SQLException {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, startNum);
                    stmt.setLong(2, size);
                    try (ResultSet res = stmt.executeQuery()) {
                        List<Entry<T>> entries = new ArrayList<>();
                        while (res.next()) {
                            String id = res.getString(1);
                            String jsonEntityVal = res.getString(2);
                            OffsetDateTime updated = (res.getTimestamp(3).toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
                            Content<T> content = new Content<T>(codec.decode(jsonEntityVal), "");
                            entries.add(new AtomEntry<>(id, updated, content));
                        }
                        return entries;
                    }
                }
            }
        };

    }

    @Override
    public CreateTablesOp createEntryTable(final Connection conn, final JdbcEntryStoreMetadata meta) {
        return new CreateTablesOp() {

            String sql = String.format(CREATE_TABLE_SQL,
                    meta.getTableName(),
                    meta.getPrimaryKeyColumnName(),
                    meta.getSequenceNoColumnName(),
                    meta.getIdColumnName(),
                    meta.getUpdatedColumnName(),
                    meta.getEntryValueColumnName());

            @Override
            public Boolean execute() throws SQLException {
                try (Statement stmt = conn.createStatement()) {
                    return stmt.execute(sql);
                }
            }
        };
    }


    @Override
    public TotalSizeOp createTotalSizeOp(final Connection conn, final JdbcEntryStoreMetadata meta) {
        final String sql = String.format( MAX_SEQNO_STATEMENT, meta.getSequenceNoColumnName(), meta.getTableName()) ;
        return new TotalSizeOp() {

            @Override
            public Long execute() throws SQLException {
                try (Statement stmt = conn.createStatement();
                     ResultSet resultSet = stmt.executeQuery(sql)) {
                    if (resultSet.next()) {
                        return resultSet.getLong(1) + 1;
                    }
                }
                return 0L;
            }

        };
    }

    @Override
    public <T> SaveEntryOp<T> createSaveEntryOp(final Connection conn, final Codec<T, String> codec, final JdbcEntryStoreMetadata meta) throws
            SQLException {

        final String sql = String.format(INSERT_STATEMENT, meta.getTableName(), meta.getIdColumnName(), meta.getUpdatedColumnName(), meta.getEntryValueColumnName());

        final PreparedStatement stmt = conn.prepareStatement(sql);

        return new SaveEntryOp<T>() {

            @Override
            public void set(Entry<T> entry) throws SQLException {
                stmt.setString(1, entry.getId());
                stmt.setTimestamp(2, new Timestamp(entry.getUpdated().toInstant().toEpochMilli()));
                stmt.setString(3, codec.encode(entry.getContent().getValue()));
            }

            @Override
            public Boolean execute() throws SQLException {
                return stmt.execute();
            }

            @Override
            public void close() {
                close(stmt);
            }
        };
    }


}

