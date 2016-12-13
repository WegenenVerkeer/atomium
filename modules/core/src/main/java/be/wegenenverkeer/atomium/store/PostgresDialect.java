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

            final String sql = "select "
                    + meta.getIdColumnName() + ", "
                    + meta.getEntryValueColumnName() + ", "
                    + meta.getUpdatedColumnName()
                    + " FROM " + meta.getTableName()
                    + " WHERE " + meta.getSequenceNoColumnName() + ">= ?"
                    + " ORDER BY " + meta.getSequenceNoColumnName()
                    + " LIMIT ?";


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
            final String sql = "CREATE TABLE IF NOT EXISTS " + meta.getTableName() + " ( "
                    + meta.getPrimaryKeyColumnName() + " SERIAL primary key, "
                    + meta.getSequenceNoColumnName() + " INT, "
                    + meta.getIdColumnName() + " VARCHAR(60), "
                    + meta.getUpdatedColumnName() + " TIMESTAMP, "
                    + meta.getEntryValueColumnName() + " TEXT )";

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
        final String sql = "select max(" + meta.getSequenceNoColumnName() + ") FROM " + meta.getTableName();
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

        final String sql = "insert into " + meta.getTableName() +
                "(" + meta.getIdColumnName() + "," + meta.getUpdatedColumnName() + "," +
                meta.getEntryValueColumnName() + ") " +
                "values(?, ?, ?)";

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

