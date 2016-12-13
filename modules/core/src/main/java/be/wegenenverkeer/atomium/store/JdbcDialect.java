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
public interface JdbcDialect {

    public String getCreateTableSql();

    public String getInsertStatement();

    public String getMaxSeqnoStatement() ;

    public String getSelectStatement() ;

    default <T> GetEntriesOp<T> createGetEntriesOp(final Connection conn, final Codec<T, String> codec, final JdbcEntryStoreMetadata meta) {

        final String sql = String.format( getSelectStatement(),
                meta.getIdColumnName(),
                meta.getEntryValueColumnName(),
                meta.getUpdatedColumnName(),
                meta.getTableName(),
                meta.getSequenceNoColumnName(),
                meta.getSequenceNoColumnName());

        return new GetEntriesOp<T>() {
            private long startNum;
            private long size;

            @Override
            public void setRange(long startNum, long size) {
                this.startNum = startNum;
                this.size = size;
            }

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
                            Content<T> content = new Content<>(codec.decode(jsonEntityVal), "");
                            entries.add(new AtomEntry<>(id, updated, content));
                        }
                        return entries;
                    }
                }
            }
        };

    }

    default CreateTablesOp createEntryTable(final Connection conn, final JdbcEntryStoreMetadata meta) {

        final String sql = String.format(getCreateTableSql(),
                meta.getTableName(),
                meta.getPrimaryKeyColumnName(),
                meta.getSequenceNoColumnName(),
                meta.getIdColumnName(),
                meta.getUpdatedColumnName(),
                meta.getEntryValueColumnName());

        return () -> {
            try (Statement stmt = conn.createStatement()) {
                return stmt.execute(sql);
            }
        };
    }


    default TotalSizeOp createTotalSizeOp(final Connection conn, final JdbcEntryStoreMetadata meta) {
        final String sql = String.format( getMaxSeqnoStatement(), meta.getSequenceNoColumnName(), meta.getTableName()) ;
        return () -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet resultSet = stmt.executeQuery(sql)) {
                if (resultSet.next()) {
                    return resultSet.getLong(1) + 1;
                }
            }
            return 0L;
        };
    }

    default <T> SaveEntryOp<T> createSaveEntryOp(final Connection conn, final Codec<T, String> codec, final JdbcEntryStoreMetadata meta) throws
            SQLException {

        final String sql = String.format(getInsertStatement(),
                meta.getTableName(),
                meta.getIdColumnName(),
                meta.getUpdatedColumnName(),
                meta.getEntryValueColumnName());

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
