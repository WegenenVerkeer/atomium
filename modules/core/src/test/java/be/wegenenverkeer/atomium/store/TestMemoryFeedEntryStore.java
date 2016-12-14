package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Event;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class TestMemoryFeedEntryStore {


    private StoreFixture<String> fixture;



    @Before
    public void setUp(){
        this.fixture = new StoreFixture<>();
    }

    @Test
    public void testMemoryFeedStore(){
        fixture.loadEntries(1000, "Test value");

        List<Event<String>> received = fixture.store.getEvents(0, 11);

        assertEquals(11, received.size());


        Stream<Integer> intStream = (IntStream.range(0, 11)).boxed();
        List<Integer> expected = intStream.collect(Collectors.toList());
        Stream<Integer> receivedIdsStream = received.stream().map(e -> Integer.parseInt(e.getId()));
        List<Integer> receivedIds = receivedIdsStream.collect(Collectors.toList());
        assertEquals(expected, receivedIds);

    }



}


