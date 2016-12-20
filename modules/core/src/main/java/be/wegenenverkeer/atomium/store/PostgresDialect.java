package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Event;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * a <code>JdbcDialect</code> implementation for Postgresql
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public class PostgresDialect implements JdbcDialect {


    public static final PostgresDialect INSTANCE = new PostgresDialect();

    final private static String CREATE_TABLE_SQL = "CREATE TABLE %s ( "
            + " %s SERIAL primary key, "
            + " %s INT, "
            + "%s VARCHAR(60), "
            + " %s TIMESTAMP, "
            + " %s TEXT )";

    final private static String INSERT_STATEMENT = "INSERT INTO %s ( %s, %s, %s) VALUES (?, ?, ?)";

    final private static String MAX_SEQNO_STATEMENT = "SELECT MAX( %s ) FROM %s";

    final private static String SELECT_STATEMENT = "SELECT %s, %s, %s FROM %s WHERE %s >= ? ORDER BY %s LIMIT ?";

    //TODO -- translate to English
    /**
     * Bouw de SQL query die gebruikt kan worden om een volgnummer veld te zetten voor een tabel met events.
     * Dat volgnummer zorgt voor een voorspelbare en consistente volgorde waarop de events in een atomium feed kopen.
     * Gebruik van deze query voorkomt tussenvoegen van events en zorgt dat de events in verschillende thread kunnen geproduceerd worden.
     * <p>
     * Deze query is gebouwd om atomic te zijn. De query bestaat uit 2 delen:
     * 1) eerst worden de nieuwe atom_entries berekend via een common table expression.
     * In Postgres kan dat via het WITH statement.
     * https://www.postgresql.org/docs/current/static/queries-with.html
     * <p>
     * * Bepaal eerst de huidige hoogste waarde van de bestaande atom entries. Dat wordt opgeslagen als max_atom_entry
     * * Bepaal dan de lijst van items die nog geen atom_entry hebben, en orden die
     * * Vervolgens bereken je voor die lijst de nieuwe atom_entry waarde
     * <p>
     * 2) dan wordt deze table gebruikt om ook effectief de tabel te updaten. In Postgres kan dat handig
     * via UPDATE ... FROM:
     * https://www.postgresql.org/docs/current/static/sql-update.html
     */
    final private static String INDEX_STATEMENT =
            "WITH\n"
                    + "    max_atom_entry -- max bepalen, basis voor zetten volgnummer, -1 als nog niet gezet zodat teller bij 0 begint\n"
                    + "  AS ( SELECT coalesce(max(${table}.${sequence-field}), -1) max_atom_entry\n"
                    + "       FROM ${table}),\n"
                    + "    to_number -- lijst met aan te passen records, moet dit apart bepalen omdat volgorde anders fout is\n"
                    + "  AS (\n"
                    + "      SELECT\n"
                    + "        ${table}.${idField},\n"
                    + "        max.max_atom_entry max_atom_entry\n"
                    + "      FROM ${table} CROSS JOIN max_atom_entry max\n"
                    + "      WHERE ${table}.${sequence-field} IS NULL\n"
                    + "      ORDER BY ${order-by}\n"
                    + "  ),\n"
                    + "    to_update -- lijst met wijzigingen opbouwen\n"
                    + "  AS (\n"
                    + "      SELECT\n"
                    + "        id,\n"
                    + "        (row_number()\n"
                    + "        OVER ()) + max_atom_entry new_value\n"
                    + "      FROM to_number\n"
                    + "      ORDER BY id ASC\n"
                    + "  )\n"
                    + "-- wijzigingen toepassen\n"
                    + "UPDATE ${table}\n"
                    + "SET ${sequence-field} = to_update.new_value\n"
                    + "FROM to_update\n"
                    + "WHERE ${table}.id = to_update.id;";


    @Override
    public <T> GetEventsOp<T> mkGetEventsOp(final Connection conn, final Codec<T, String> codec, final JdbcEventStoreMetadata meta) {

        final String sql = String.format(SELECT_STATEMENT,
                meta.getIdColumnName(),
                meta.getEntryValueColumnName(),
                meta.getUpdatedColumnName(),
                meta.getTableName(),
                meta.getSequenceNoColumnName(),
                meta.getSequenceNoColumnName());

        return new GetEventsOp<T>() {
            private long startNum;
            private long size;

            @Override
            public void setRange(long startNum, long size) {
                this.startNum = startNum;
                this.size = size;
            }

            @Override
            public List<Event<T>> execute() throws SQLException {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, startNum);
                    stmt.setLong(2, size);
                    try (ResultSet res = stmt.executeQuery()) {
                        List<Event<T>> entries = new ArrayList<>();
                        while (res.next()) {
                            String id = res.getString(1);
                            String jsonEntityVal = res.getString(2);
                            OffsetDateTime updated = (res.getTimestamp(3).toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
                            T val = codec.decode(jsonEntityVal);
                            entries.add(Event.make(id, val, updated));
                        }
                        return entries;
                    }
                }
            }
        };

    }

    @Override
    public CreateEventTableOp mkCreateEventTableOp(final Connection conn, final JdbcEventStoreMetadata meta) {

        final String sql = String.format(CREATE_TABLE_SQL,
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


    @Override
    public TotalSizeOp mkTotalSizeOp(final Connection conn, final JdbcEventStoreMetadata meta) {
        final String sql = String.format(MAX_SEQNO_STATEMENT, meta.getSequenceNoColumnName(), meta.getTableName());
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

    @Override
    public <T> SaveEventOp<T> mkSaveEventOp(final Connection conn, final Codec<T, String> codec, final JdbcEventStoreMetadata meta) throws
            SQLException {

        final String sql = String.format(INSERT_STATEMENT,
                meta.getTableName(),
                meta.getIdColumnName(),
                meta.getUpdatedColumnName(),
                meta.getEntryValueColumnName());

        final PreparedStatement stmt = conn.prepareStatement(sql);

        return new SaveEventOp<T>() {

            @Override
            public void set(Event<T> ev) throws SQLException {
                stmt.setString(1, ev.getId());
                stmt.setTimestamp(2, new Timestamp(ev.getUpdated().toInstant().toEpochMilli()));
                stmt.setString(3, codec.encode(ev.getValue()));
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

    @Override
    public IndexOp mkIndexOp(final Connection conn, final JdbcEventStoreMetadata meta) {
        final String sql = INDEX_STATEMENT
                .replace("${table}", meta.getTableName())
                .replace("${sequence-field}", meta.getSequenceNoColumnName())
                .replace("${idField}", meta.getPrimaryKeyColumnName())
                .replace("${order-by}", meta.getPrimaryKeyColumnName());

        return () -> {
            try (Statement stmt = conn.createStatement()) {
                return stmt.execute(sql);
            }
        };
    }


}

