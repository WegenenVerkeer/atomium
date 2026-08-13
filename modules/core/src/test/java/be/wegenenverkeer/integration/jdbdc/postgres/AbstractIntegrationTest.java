package be.wegenenverkeer.integration.jdbdc.postgres;

import be.wegenenverkeer.atomium.store.CreateEventTableOp;
import be.wegenenverkeer.atomium.store.JdbcDialect;
import be.wegenenverkeer.atomium.store.JdbcEventStoreMetadata;
import be.wegenenverkeer.atomium.store.PostgresDialect;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * Abstract base class for integration tests against a Postgres database.
 * <p>
 * We assume that the provided docker image (see the  docker directory) is running
 *
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 13/12/16.
 */
public abstract class AbstractIntegrationTest {

    static final String TEST_SCHEMA = "ATTEST";
    static final Logger LOG = LoggerFactory.getLogger("chapters.introduction.HelloWorld1");

    static Driver postgresDriver;
    static JdbcDialect dialect = PostgresDialect.INSTANCE;
    static String databaseUrl = "jdbc:tc:postgresql:9.6:///atomium";
    static Properties dbProps = new Properties();


    static JdbcEventStoreMetadata metadata =
            new JdbcEventStoreMetadata(
                    "events",
                    "event_id",
                    "updated",
                    "id",
                    "seqno",
                    "json",
                    "jsonb"
            );

    boolean withTableCreation() {
        return false;
    }

    // Testcontainers 2 dropped JUnit 4 rule support, so we drive the lifecycle ourselves. Still one
    // container per test method, as @Rule gave us: TestJdbcDao creates its table in its own @Before,
    // which JUnit runs after this one because it is declared in the subclass.
    // postgres:9.6.12 was what the removed no-arg constructor defaulted to.
    public PostgreSQLContainer container;

    @Before
    public void startContainer() {
        container = new PostgreSQLContainer("postgres:9.6.12");
        container.start();
    }

    @After
    public void stopContainer() {
        if (container != null) {
            container.stop();
        }
    }

    public Connection mkConnection() {
        try {
            return DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
        } catch (SQLException t) {
            throw new RuntimeException(t);
        }

    }

}
