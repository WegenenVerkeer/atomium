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
 * a <code>JdbcDialect</code> implementation for Postgresql
 *
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public class PostgresDialect implements JdbcDialect {


    public static final PostgresDialect INSTANCE = new PostgresDialect();

    final private static String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS %s ( "
            + " %s SERIAL primary key, "
            + " %s INT, "
            +  "%s VARCHAR(60), "
            + " %s TIMESTAMP, "
            + " %s TEXT )";

    final private static String INSERT_STATEMENT = "INSERT INTO %s ( %s, %s, %s) VALUES (?, ?, ?)";

    final private static String MAX_SEQNO_STATEMENT = "SELECT MAX( %s ) FROM %s";

    final private static String SELECT_STATEMENT = "SELECT %s, %s, %s FROM %s WHERE %s >= ? ORDER BY %s LIMIT ?";


    @Override
    public String getCreateTableSql() {
        return CREATE_TABLE_SQL;
    }

    @Override
    public String getInsertStatement() {
        return INSERT_STATEMENT;
    }

    @Override
    public String getMaxSeqnoStatement() {
        return MAX_SEQNO_STATEMENT;
    }

    @Override
    public String getSelectStatement() {
        return SELECT_STATEMENT;
    }




}

