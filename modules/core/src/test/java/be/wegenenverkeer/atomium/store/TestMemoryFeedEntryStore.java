package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Entry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        CollectSubscriber<Entry<String>> s = new CollectSubscriber<>();
        fixture.store.getEntries(0, 11).subscribe( s );
        s.request(5);
        assertEquals(5, s.received.size());
        s.request(6);
        assertEquals(11, s.received.size());
        assertTrue(s.isCompleted);
        Stream<Integer> integerStream = s.received.stream().map(e -> Integer.parseInt(e.getId()));
        List<Integer> received = integerStream.collect(Collectors.toList());
        Stream<Integer> intStream = (IntStream.range(0, 11)).boxed();
        List<Integer> expected = intStream.collect(Collectors.toList());
        assertEquals(expected, received);

    }


    @Test
    public void testMemoryFeedStoreCanRequestMore(){
        fixture.loadEntries(5, "Test value");
        CollectSubscriber<Entry<String>> s = new CollectSubscriber<>();
        fixture.store.getEntries(0, 11).subscribe( s );
        s.request(100);
        assertTrue(s.isCompleted);
    }



}


