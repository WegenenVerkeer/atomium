package be.wegenenverkeer.integration.jdbdc.postgres;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Event;
import be.wegenenverkeer.atomium.api.EventDao;
import be.wegenenverkeer.atomium.format.JacksonCodec;
import be.wegenenverkeer.atomium.store.CreateEventTableOp;
import be.wegenenverkeer.atomium.store.PostgresEventStore;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Karel Maesen, Geovise BVBA on 13/12/16.
 */
public class TestJdbcDao extends AbstractIntegrationTest {

    Codec<TestVal, String> codec = new JacksonCodec<>(TestVal.class);
    PostgresEventStore<TestVal> store = new PostgresEventStore<>(metadata, codec);

    @Before
    public void init() throws SQLException {
        try(Connection connection = mkConnection()) {
            CreateEventTableOp op = dialect.mkCreateEventTableOp(connection, metadata);
            op.execute();
        }
    }

    @Override
    boolean withTableCreation() {
        return true;
    }

    @Test
    public void writingEvents() throws SQLException {

        List<Event<TestVal>> entries = new ArrayList<>();
        try (Connection conn = mkConnection()) {
            EventDao<TestVal> dao = store.createDao(conn);
            entries.add(Event.make( "0", new TestVal("test 0"), OffsetDateTime.now()));
            entries.add(Event.make( "1", new TestVal("test 1"), OffsetDateTime.now()));
            entries.add(Event.make( "2", new TestVal("test 2"), OffsetDateTime.now()));
            entries.add(Event.make( "3", new TestVal("test 3"), OffsetDateTime.now()));
            dao.push(entries);
        }

        try (Connection conn = mkConnection()) {
            EventDao<TestVal> dao = store.createDao(conn);
            List<Event<TestVal>> list = dao.getEvents(0, 5);
            assertTrue(list.isEmpty());
        }

        try (Connection conn = mkConnection()) {
            List<Event<TestVal>> list = store.indexAndRetrieve(conn, 0, 5);
            assertEquals(4, list.size());
            assertEquals(entries, list);
        }

        try (Connection conn = mkConnection()) {
            List<Event<TestVal>> list = store.indexAndRetrieve(conn, 0, 2);
            assertEquals(2, list.size());
            assertEquals(entries.subList(0, 2), list);
        }

    }


}

class TestVal {


    private String value;

    private TestVal(){} //required for jackson


    public TestVal(String v) {
        this.value = v;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestVal testVal = (TestVal) o;

        return value != null ? value.equals(testVal.value) : testVal.value == null;

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TestVal{" +
                "value='" + value + '\'' +
                '}';
    }
}
