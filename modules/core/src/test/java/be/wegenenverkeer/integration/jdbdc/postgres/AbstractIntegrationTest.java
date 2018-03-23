package be.wegenenverkeer.integration.jdbdc.postgres;

import be.wegenenverkeer.atomium.store.CreateEventTableOp;
import be.wegenenverkeer.atomium.store.JdbcDialect;
import be.wegenenverkeer.atomium.store.JdbcEventStoreMetadata;
import be.wegenenverkeer.atomium.store.PostgresDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Abstract base class for integration tests against a Postgres database.
 * <p>
 * We assume that PG environment vars are suitably set when tests are run so that proper connection to the database will be establised
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 13/12/16.
 */
public abstract class AbstractIntegrationTest {

    static final String TEST_SCHEMA = "ATTEST";
    static final Logger LOG = LoggerFactory.getLogger("chapters.introduction.HelloWorld1");

    static Driver postgresDriver;
    static JdbcDialect dialect = PostgresDialect.INSTANCE;
    static String databaseUrl = "jdbc:postgresql://localhost/atomium_test";

    JdbcEventStoreMetadata metadata =
            new JdbcEventStoreMetadata(
                    "events",
                    "event_id",
                    "updated",
                    "id",
                    "seqno",
                    "json",
                    "jsonb"
            );

    boolean withTableCreation(){
        return false;
    }

    @BeforeClass
    public static void loadDriver() {
        try {
            postgresDriver = java.sql.DriverManager.getDriver(databaseUrl);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void createSchemaAndTable() {
        doSql(String.format("CREATE SCHEMA IF NOT EXISTS %s", TEST_SCHEMA));
        if (withTableCreation()) {
            try (Connection conn = mkConnection(TEST_SCHEMA); CreateEventTableOp op = dialect.mkCreateEventTableOp(conn, metadata)) {
                op.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @After
    public void dropSchema() {
        doSql(String.format("DROP SCHEMA %s CASCADE", TEST_SCHEMA));
    }

    void doSql(String sql) {
        doSql(sql, null);
    }

    void doSql(String sql, String schema) {

        try (Connection connection = mkConnection(schema);
             Statement stmt = connection.createStatement()) {
            LOG.info("Executing SQL: " + sql);
            stmt.execute(sql);
        } catch (SQLException e) {
            LOG.warn("SQL Execution failure: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    Connection mkConnection(String schema) throws SQLException {
        Connection conn;
        if (schema == null ) {
            conn  = postgresDriver.connect(databaseUrl, null);
        } else {
            Properties props = new Properties();
            props.put("currentSchema", schema);
            conn =  postgresDriver.connect(databaseUrl, props);
        }
        conn.setAutoCommit(true);
        return conn;
    }


}
