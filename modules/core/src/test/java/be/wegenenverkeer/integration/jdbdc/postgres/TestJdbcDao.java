package be.wegenenverkeer.integration.jdbdc.postgres;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.EntryDao;
import be.wegenenverkeer.atomium.format.AtomEntry;
import be.wegenenverkeer.atomium.format.Content;
import be.wegenenverkeer.atomium.format.JacksonCodec;
import be.wegenenverkeer.atomium.store.PostgresEntryStore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Karel Maesen, Geovise BVBA on 13/12/16.
 */
public class TestJdbcDao extends AbstractIntegrationTest {

    Codec<Event, String> codec = new JacksonCodec<>(Event.class);
    PostgresEntryStore<Event> store = new PostgresEntryStore<>(metadata, codec);

    @Override
    boolean withTableCreation() {
        return true;
    }

    @Test
    public void writingEvents() throws SQLException {
        List<Entry<Event>> entries = new ArrayList<Entry<Event>>();
        try (Connection conn = mkConnection(TEST_SCHEMA)) {
            EntryDao<Event> dao = store.createDao(conn);
            entries.add(new AtomEntry<Event>("0", new Content<>(new Event("test 0"), "")));
            entries.add(new AtomEntry<Event>("1", new Content<>(new Event("test 1"), "")));
            entries.add(new AtomEntry<Event>("2", new Content<>(new Event("test 2"), "")));
            entries.add(new AtomEntry<Event>("3", new Content<>(new Event("test "), "")));
            dao.push(entries);
        }

        try (Connection conn = mkConnection(TEST_SCHEMA)) {
            EntryDao<Event> dao = store.createDao(conn);
            List<Entry<Event>> list = dao.getEntries(0, 5);
            assertTrue(list.isEmpty());
        }

        try (Connection conn = mkConnection(TEST_SCHEMA)) {
            List<Entry<Event>> list = store.indexAndRetrieve(conn, 0, 5);
            assertEquals(4, list.size());
            assertEquals(entries, list);
        }

        try (Connection conn = mkConnection(TEST_SCHEMA)) {
            List<Entry<Event>> list = store.indexAndRetrieve(conn, 0, 2);
            assertEquals(2, list.size());
            assertEquals(entries.subList(0, 2), list);
        }

    }


}

class Event {


    private String value;

    private Event(){} //required for jackson


    public Event(String v) {
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

        Event event = (Event) o;

        return value != null ? value.equals(event.value) : event.value == null;

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
