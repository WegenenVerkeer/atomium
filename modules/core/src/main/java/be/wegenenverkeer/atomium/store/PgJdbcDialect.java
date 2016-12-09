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
public class PgJdbcDialect implements JdbcDialect {


    public static final PgJdbcDialect INSTANCE = new PgJdbcDialect();


    @Override
    public <T> JdbcGetEntriesOp createGetEntriesOp(final Connection conn, final Codec<T, String> codec, final JdbcEntryStoreMetadata meta) {

        return new JdbcGetEntriesOp<T>() {
            private long startNum;
            private long size;
            private PreparedStatement stmt;

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
                stmt = conn.prepareStatement(sql);
                stmt.setLong(1, startNum);
                stmt.setLong(2, size);
                ResultSet res = null;
                try {
                    res = stmt.executeQuery();
                    List<Entry<T>> entries = new ArrayList<>();
                    while (res.next()) {
                        String id = res.getString(1);
                        String jsonEntityVal = res.getString(2);
                        OffsetDateTime updated = (res.getTimestamp(3).toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
                        Content<T> content = new Content<T>(codec.decode(jsonEntityVal), "");
                        entries.add(new AtomEntry<>(id, updated, content));
                    }
                    return entries;
                } finally {
                    if (res != null) {
                        try {
                            res.close();
                        } catch (Throwable t) {
                        }
                    }
                }
            }

            @Override
            public void close() {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        // do nothing
                    }
                }
            }
        };
    }


    @Override
    public JdbcTotalSizeOp createTotalSizeOp(final Connection conn, final JdbcEntryStoreMetadata meta)  {
        final String sql = "select max(" + meta.getSequenceNoColumnName() + ") FROM " + meta.getTableName();
        return new JdbcTotalSizeOp() {
            private Statement stmt;
            @Override
            public Long execute() throws SQLException {
                stmt = conn.createStatement();
                ResultSet resultSet = null;
                try {
                    resultSet = stmt.executeQuery(sql);
                    if (resultSet.next()) {
                        return resultSet.getLong(1) + 1;
                    }
                } finally {
                    if (resultSet != null ) {
                        try {
                            resultSet.close();

                        } catch (SQLException e) {}
                    }
                }
                return 0L;
            }

            @Override
            public void close() {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // do nothing
                }
            }
        };
    }

    @Override
    public <T> JdbcSaveEntryOp createSaveEntryOp(final Connection conn, final Codec<T, String> codec, final JdbcEntryStoreMetadata meta) throws SQLException {

        final String sql = "insert into " + meta.getTableName() +
                "(" + meta.getIdColumnName() + "," + meta.getUpdatedColumnName() + "," +
                meta.getEntryValueColumnName() + ") " +
                "values(?, ?, ?)";

        final PreparedStatement statement = conn.prepareStatement(sql);

        return new JdbcSaveEntryOp<T>() {

            @Override
            public void set(Entry<T> entry) throws SQLException {
                statement.setString(1, entry.getId());
                statement.setTimestamp(2, new Timestamp(entry.getUpdated().toInstant().toEpochMilli()));
                statement.setString(3, codec.encode(entry.getContent().getValue()));
            }

            @Override
            public Boolean execute() throws SQLException {
                return statement.execute();
            }

            @Override
            public void close() {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        // do nothing
                    }
                }
            }
        };
    }


}

