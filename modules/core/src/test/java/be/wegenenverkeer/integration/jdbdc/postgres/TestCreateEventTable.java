package be.wegenenverkeer.integration.jdbdc.postgres;

import be.wegenenverkeer.atomium.store.JdbcCreateTablesOp;
import be.wegenenverkeer.atomium.store.JdbcDialect;
import be.wegenenverkeer.atomium.store.PgJdbcDialect;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 13/12/16.
 */
public class TestCreateEventTable extends AbstractIntegrationTest {

    @Test
    public void testCreate() {
        try (Connection conn = mkConnection(TEST_SCHEMA); JdbcCreateTablesOp op = dialect.createEntryTable(conn, metadata)) {
            op.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
